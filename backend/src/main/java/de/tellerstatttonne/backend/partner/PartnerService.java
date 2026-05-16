package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.partner.note.CreatePartnerNoteRequest;
import de.tellerstatttonne.backend.partner.note.PartnerNoteService;
import de.tellerstatttonne.backend.partner.note.Visibility;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupEntity;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import de.tellerstatttonne.backend.user.availability.UserAvailabilityService;
import de.tellerstatttonne.backend.user.availability.UserAvailabilityService.SlotKey;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerService {

    private final PartnerRepository repository;
    private final GeocodingService geocodingService;
    private final UserAvailabilityService availabilityService;
    private final PickupRepository pickupRepository;
    private final PartnerNoteService partnerNoteService;
    private final ApplicationEventPublisher eventPublisher;

    public PartnerService(
        PartnerRepository repository,
        GeocodingService geocodingService,
        UserAvailabilityService availabilityService,
        PickupRepository pickupRepository,
        PartnerNoteService partnerNoteService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.geocodingService = geocodingService;
        this.availabilityService = availabilityService;
        this.pickupRepository = pickupRepository;
        this.partnerNoteService = partnerNoteService;
        this.eventPublisher = eventPublisher;
    }

    private static Long currentActorId() {
        try {
            return CurrentUser.requireId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static final Map<Partner.Status, String> STATUS_LABELS;
    static {
        STATUS_LABELS = new EnumMap<>(Partner.Status.class);
        STATUS_LABELS.put(Partner.Status.KEIN_KONTAKT, "Kein Kontakt");
        STATUS_LABELS.put(Partner.Status.VERHANDLUNGEN_LAUFEN, "Verhandlungen laufen");
        STATUS_LABELS.put(Partner.Status.WILL_NICHT_KOOPERIEREN, "Will nicht kooperieren");
        STATUS_LABELS.put(Partner.Status.KOOPERIERT, "Kooperiert mit uns");
        STATUS_LABELS.put(Partner.Status.KOOPERIERT_FOODSHARING, "Kooperiert mit Foodsharing");
        STATUS_LABELS.put(Partner.Status.SPENDET_AN_TAFEL, "Spendet an Tafel etc.");
        STATUS_LABELS.put(Partner.Status.EXISTIERT_NICHT_MEHR, "Existiert nicht mehr");
    }

    @Transactional(readOnly = true)
    public List<Partner> findAll() {
        List<Partner> partners = repository.findAllByStatusNot(Partner.Status.EXISTIERT_NICHT_MEHR).stream()
            .map(PartnerMapper::toDto).toList();
        return enrichWithAvailability(partners);
    }

    @Transactional(readOnly = true)
    public List<Partner> findAllForMember(Long userId) {
        List<Partner> partners = repository.findAllByMemberIdAndStatusNot(userId, Partner.Status.EXISTIERT_NICHT_MEHR).stream()
            .map(PartnerMapper::toDto).toList();
        return enrichWithAvailability(partners);
    }

    @Transactional(readOnly = true)
    public List<Partner> findAllDeleted() {
        return repository.findAllByStatus(Partner.Status.EXISTIERT_NICHT_MEHR).stream()
            .map(PartnerMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Partner> findById(Long id) {
        return repository.findById(id)
            .map(PartnerMapper::toDto)
            .map(p -> enrichWithAvailability(List.of(p)).get(0));
    }

    private List<Partner> enrichWithAvailability(List<Partner> partners) {
        LinkedHashSet<SlotKey> slotKeys = new LinkedHashSet<>();
        for (Partner p : partners) {
            if (p.pickupSlots() == null) continue;
            for (Partner.PickupSlot s : p.pickupSlots()) {
                if (s.weekday() != null && s.startTime() != null && s.endTime() != null) {
                    slotKeys.add(new SlotKey(s.weekday(), s.startTime(), s.endTime()));
                }
            }
        }
        if (slotKeys.isEmpty()) return partners;
        Map<SlotKey, Integer> counts = availabilityService.countAvailableForSlots(new ArrayList<>(slotKeys));
        return partners.stream().map(p -> withSlotCounts(p, counts)).toList();
    }

    private static Partner withSlotCounts(Partner p, Map<SlotKey, Integer> counts) {
        if (p.pickupSlots() == null || p.pickupSlots().isEmpty()) return p;
        List<Partner.PickupSlot> enriched = p.pickupSlots().stream()
            .map(s -> {
                Integer count = counts.get(new SlotKey(s.weekday(), s.startTime(), s.endTime()));
                return new Partner.PickupSlot(
                    s.weekday(), s.startTime(), s.endTime(), s.active(),
                    s.capacity(), s.expectedKg(), count != null ? count : 0
                );
            })
            .toList();
        return new Partner(
            p.id(), p.name(), p.categoryId(), p.street(), p.postalCode(), p.city(),
            p.logoUrl(), p.contact(), p.retterContact(), enriched, p.status(), p.latitude(), p.longitude(),
            p.parkingInfo(), p.accessInstructions(), p.pickupProcedure(), p.onSiteContactNote(),
            p.preferredFoodCategoryIds()
        );
    }

    public Partner create(Partner partner) {
        validate(partner);
        PartnerEntity entity = new PartnerEntity();
        PartnerMapper.applyToEntity(entity, partner);
        if (hasCoordinates(partner)) {
            entity.setLatitude(partner.latitude());
            entity.setLongitude(partner.longitude());
        } else {
            applyForwardGeocoding(entity);
        }
        return PartnerMapper.toDto(repository.save(entity));
    }

    public Optional<Partner> update(Long id, Partner partner) {
        return repository.findById(id).map(entity -> {
            validate(partner);
            checkSlotsNotInUse(id, entity, partner);
            Partner.Status oldStatus = entity.getStatus();
            boolean addressChanged = !addressEquals(entity, partner);
            PartnerMapper.applyToEntity(entity, partner);
            if (hasCoordinates(partner)) {
                entity.setLatitude(partner.latitude());
                entity.setLongitude(partner.longitude());
            } else if (addressChanged || entity.getLatitude() == null || entity.getLongitude() == null) {
                applyForwardGeocoding(entity);
            }
            PartnerEntity saved = repository.save(entity);
            recordStatusChange(id, oldStatus, saved.getStatus());
            return PartnerMapper.toDto(saved);
        });
    }

    public Optional<Partner> updateLogoUrl(Long id, String logoUrl) {
        return repository.findById(id).map(entity -> {
            entity.setLogoUrl(logoUrl);
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> findLogoUrl(Long id) {
        return repository.findById(id).map(PartnerEntity::getLogoUrl);
    }

    public Optional<Partner> regeocode(Long id) {
        return repository.findById(id).map(entity -> {
            applyForwardGeocoding(entity);
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(entity -> {
            Partner.Status oldStatus = entity.getStatus();
            entity.setStatus(Partner.Status.EXISTIERT_NICHT_MEHR);
            repository.save(entity);
            recordStatusChange(id, oldStatus, Partner.Status.EXISTIERT_NICHT_MEHR);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.STORE_DELETED)
                .actorUserId(currentActorId())
                .target("PARTNER", id)
                .message("Betrieb geloescht: " + entity.getName())
                .build());
            return true;
        }).orElse(false);
    }

    public Optional<Partner> restore(Long id) {
        return repository.findById(id).map(entity -> {
            Partner.Status oldStatus = entity.getStatus();
            entity.setStatus(Partner.Status.KEIN_KONTAKT);
            PartnerEntity saved = repository.save(entity);
            recordStatusChange(id, oldStatus, Partner.Status.KEIN_KONTAKT);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.STORE_RESTORED)
                .actorUserId(currentActorId())
                .target("PARTNER", id)
                .message("Betrieb wiederhergestellt: " + saved.getName())
                .build());
            return PartnerMapper.toDto(saved);
        });
    }

    private void recordStatusChange(Long partnerId, Partner.Status oldStatus, Partner.Status newStatus) {
        if (oldStatus == newStatus) return;
        String body = "Status geändert: " + label(oldStatus) + " → " + label(newStatus);
        partnerNoteService.create(
            partnerId,
            new CreatePartnerNoteRequest(body, Visibility.INTERNAL),
            CurrentUser.requireId()
        );
    }

    private static String label(Partner.Status status) {
        String label = STATUS_LABELS.get(status);
        return label != null ? label : status.name();
    }

    private static boolean hasCoordinates(Partner partner) {
        return partner.latitude() != null && partner.longitude() != null;
    }

    private void applyForwardGeocoding(PartnerEntity entity) {
        Optional<GeocodingService.Coordinates> coords = geocodingService.geocode(
            entity.getStreet(), entity.getPostalCode(), entity.getCity());
        if (coords.isPresent()) {
            entity.setLatitude(coords.get().lat());
            entity.setLongitude(coords.get().lon());
        } else {
            entity.setLatitude(null);
            entity.setLongitude(null);
        }
    }

    private boolean addressEquals(PartnerEntity entity, Partner partner) {
        return Objects.equals(entity.getStreet(), partner.street())
            && Objects.equals(entity.getPostalCode(), partner.postalCode())
            && Objects.equals(entity.getCity(), partner.city());
    }

    private void checkSlotsNotInUse(Long partnerId, PartnerEntity existing, Partner incoming) {
        Set<SlotKey> incomingKeys = incoming.pickupSlots() == null
            ? Set.of()
            : incoming.pickupSlots().stream()
                .map(s -> new SlotKey(s.weekday(), s.startTime(), s.endTime()))
                .collect(Collectors.toCollection(HashSet::new));

        List<PartnerEntity.PickupSlotEmbeddable> removed = existing.getPickupSlots().stream()
            .filter(s -> !incomingKeys.contains(new SlotKey(s.getWeekday(), s.getStartTime(), s.getEndTime())))
            .toList();
        if (removed.isEmpty()) return;

        List<PickupEntity> futurePickups = pickupRepository
            .findByPartnerIdAndStatusAndDateGreaterThanEqual(partnerId, Pickup.Status.SCHEDULED, LocalDate.now());
        if (futurePickups.isEmpty()) return;

        Set<SlotKey> bookedKeys = futurePickups.stream()
            .map(p -> new SlotKey(toWeekday(p.getDate().getDayOfWeek()), p.getStartTime(), p.getEndTime()))
            .collect(Collectors.toSet());

        List<String> conflicts = removed.stream()
            .filter(s -> bookedKeys.contains(new SlotKey(s.getWeekday(), s.getStartTime(), s.getEndTime())))
            .map(s -> s.getWeekday() + " " + s.getStartTime() + "–" + s.getEndTime())
            .toList();
        if (!conflicts.isEmpty()) {
            throw new SlotInUseException(
                "Abholzeit wird noch von geplanten Abholungen genutzt: " + String.join(", ", conflicts));
        }
    }

    private static Partner.Weekday toWeekday(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> Partner.Weekday.MONDAY;
            case TUESDAY -> Partner.Weekday.TUESDAY;
            case WEDNESDAY -> Partner.Weekday.WEDNESDAY;
            case THURSDAY -> Partner.Weekday.THURSDAY;
            case FRIDAY -> Partner.Weekday.FRIDAY;
            case SATURDAY -> Partner.Weekday.SATURDAY;
            case SUNDAY -> Partner.Weekday.SUNDAY;
        };
    }

    public static class SlotInUseException extends RuntimeException {
        public SlotInUseException(String message) { super(message); }
    }

    private void validate(Partner partner) {
        if (partner.name() == null || partner.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (partner.categoryId() == null) {
            throw new IllegalArgumentException("categoryId is required");
        }
    }
}
