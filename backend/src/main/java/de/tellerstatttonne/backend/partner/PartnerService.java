package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupEntity;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.user.availability.UserAvailabilityService;
import de.tellerstatttonne.backend.user.availability.UserAvailabilityService.SlotKey;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerService {

    private final PartnerRepository repository;
    private final GeocodingService geocodingService;
    private final UserAvailabilityService availabilityService;
    private final PickupRepository pickupRepository;

    public PartnerService(
        PartnerRepository repository,
        GeocodingService geocodingService,
        UserAvailabilityService availabilityService,
        PickupRepository pickupRepository
    ) {
        this.repository = repository;
        this.geocodingService = geocodingService;
        this.availabilityService = availabilityService;
        this.pickupRepository = pickupRepository;
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
                    s.capacity(), count != null ? count : 0
                );
            })
            .toList();
        return new Partner(
            p.id(), p.name(), p.category(), p.street(), p.postalCode(), p.city(),
            p.logoUrl(), p.contact(), enriched, p.status(), p.latitude(), p.longitude()
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
            boolean addressChanged = !addressEquals(entity, partner);
            PartnerMapper.applyToEntity(entity, partner);
            if (hasCoordinates(partner)) {
                entity.setLatitude(partner.latitude());
                entity.setLongitude(partner.longitude());
            } else if (addressChanged || entity.getLatitude() == null || entity.getLongitude() == null) {
                applyForwardGeocoding(entity);
            }
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    public Optional<Partner> regeocode(Long id) {
        return repository.findById(id).map(entity -> {
            applyForwardGeocoding(entity);
            return PartnerMapper.toDto(repository.save(entity));
        });
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(entity -> {
            entity.setStatus(Partner.Status.EXISTIERT_NICHT_MEHR);
            repository.save(entity);
            return true;
        }).orElse(false);
    }

    public Optional<Partner> restore(Long id) {
        return repository.findById(id).map(entity -> {
            entity.setStatus(Partner.Status.KEIN_KONTAKT);
            return PartnerMapper.toDto(repository.save(entity));
        });
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
        if (partner.category() == null) {
            throw new IllegalArgumentException("category is required");
        }
    }
}
