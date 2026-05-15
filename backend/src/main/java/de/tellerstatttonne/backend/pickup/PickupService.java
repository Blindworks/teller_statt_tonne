package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.event.EventEntity;
import de.tellerstatttonne.backend.event.EventRepository;
import de.tellerstatttonne.backend.notification.event.PickupStatusChangedEvent;
import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.user.User;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import de.tellerstatttonne.backend.user.UserService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PickupService {

    private static final Pattern TIME = Pattern.compile("^\\d{2}:\\d{2}$");

    private final PickupRepository repository;
    private final PartnerRepository partnerRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public PickupService(PickupRepository repository,
                         PartnerRepository partnerRepository,
                         EventRepository eventRepository,
                         UserRepository userRepository,
                         UserService userService,
                         ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Pickup> findBetween(LocalDate from, LocalDate to) {
        Optional<Set<Long>> filter = currentUserPartnerFilter();
        List<PickupEntity> entities = repository.findByDateBetweenOrderByDateAscStartTimeAsc(from, to);
        return mapAll(applyFilter(entities, filter));
    }

    @Transactional(readOnly = true)
    public List<Pickup> findRecent() {
        Optional<Set<Long>> filter = currentUserPartnerFilter();
        return mapAll(applyFilter(repository.findTop10ByOrderByDateDescStartTimeDesc(), filter));
    }

    @Transactional(readOnly = true)
    public List<Pickup> findUpcoming(int limit) {
        Optional<Set<Long>> filter = currentUserPartnerFilter();
        int capped = Math.max(1, Math.min(limit, 50));
        return mapAll(applyFilter(repository.findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            Pickup.Status.SCHEDULED, LocalDate.now(), PageRequest.of(0, capped)), filter));
    }

    @Transactional(readOnly = true)
    public Optional<Pickup> findById(Long id) {
        Optional<Set<Long>> filter = currentUserPartnerFilter();
        return repository.findById(id)
            .filter(e -> isAllowed(e, filter))
            .map(e -> PickupMapper.toDto(e, resolveUsers(List.of(e))));
    }

    private static boolean isAllowed(PickupEntity e, Optional<Set<Long>> filter) {
        if (filter.isEmpty()) return true;
        if (e.getEvent() != null) return true;
        return e.getPartner() != null && filter.get().contains(e.getPartner().getId());
    }

    private Optional<Set<Long>> currentUserPartnerFilter() {
        Long userId;
        try {
            userId = CurrentUser.requireId();
        } catch (IllegalStateException ex) {
            return Optional.empty();
        }
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) return Optional.of(Set.of());
        if (user.hasRole("ADMINISTRATOR") || user.hasRole("TEAMLEITER")) return Optional.empty();
        if (user.hasRole("RETTER")) {
            return Optional.of(new HashSet<>(partnerRepository.findIdsByMemberId(userId)));
        }
        return Optional.of(Set.of());
    }

    private static List<PickupEntity> applyFilter(List<PickupEntity> entities, Optional<Set<Long>> filter) {
        if (filter.isEmpty()) return entities;
        return entities.stream()
            .filter(e -> isAllowed(e, filter))
            .toList();
    }

    public Pickup create(Pickup pickup) {
        validate(pickup);
        PartnerEntity partner = null;
        EventEntity event = null;
        if (pickup.eventId() != null) {
            event = loadEvent(pickup.eventId());
            if (repository.existsEventPickupOverlap(
                    pickup.eventId(), pickup.date(), pickup.startTime(), pickup.endTime())) {
                throw new IllegalStateException(
                    "Für diese Sonderabholung existiert am " + pickup.date()
                        + " bereits ein Termin im Zeitraum "
                        + pickup.startTime() + "–" + pickup.endTime() + " Uhr.");
            }
        } else {
            partner = loadPartner(pickup.partnerId());
            if (partner.getStatus() != Partner.Status.KOOPERIERT) {
                throw new IllegalArgumentException(
                    "Pickups können nur für kooperierende Betriebe angelegt werden");
            }
        }
        PickupEntity entity = new PickupEntity();
        PickupMapper.applyToEntity(entity, pickup, partner, event);
        if (entity.getSavedKg() == null && partner != null) {
            entity.setSavedKg(resolveSlotExpectedKg(partner, pickup));
        }
        PickupEntity saved = repository.save(entity);
        return PickupMapper.toDto(saved, resolveUsers(List.of(saved)));
    }

    private static java.math.BigDecimal resolveSlotExpectedKg(PartnerEntity partner, Pickup pickup) {
        if (partner.getPickupSlots() == null || pickup.date() == null) return null;
        Partner.Weekday weekday = WEEKDAY_BY_DAY_OF_WEEK[pickup.date().getDayOfWeek().getValue() - 1];
        return partner.getPickupSlots().stream()
            .filter(s -> s.getWeekday() == weekday)
            .filter(s -> java.util.Objects.equals(s.getStartTime(), pickup.startTime()))
            .filter(s -> java.util.Objects.equals(s.getEndTime(), pickup.endTime()))
            .map(PartnerEntity.PickupSlotEmbeddable::getExpectedKg)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private static final Partner.Weekday[] WEEKDAY_BY_DAY_OF_WEEK = {
        Partner.Weekday.MONDAY, Partner.Weekday.TUESDAY, Partner.Weekday.WEDNESDAY,
        Partner.Weekday.THURSDAY, Partner.Weekday.FRIDAY, Partner.Weekday.SATURDAY,
        Partner.Weekday.SUNDAY
    };

    public List<Pickup> createSeries(Pickup template, LocalDate until) {
        if (template.eventId() != null) {
            throw new IllegalArgumentException("series creation is not supported for event pickups");
        }
        if (template.date() == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (until == null || until.isBefore(template.date())) {
            throw new IllegalArgumentException("until must be on or after the start date");
        }
        LocalDate maxUntil = template.date().plusWeeks(26);
        if (until.isAfter(maxUntil)) {
            throw new IllegalArgumentException("until must be at most 26 weeks after the start date");
        }
        java.util.List<Pickup> created = new java.util.ArrayList<>();
        LocalDate cursor = template.date();
        while (!cursor.isAfter(until)) {
            Pickup occurrence = new Pickup(
                null, template.partnerId(), template.partnerName(), template.partnerCategoryId(),
                template.partnerStreet(), template.partnerCity(), template.partnerLogoUrl(),
                null, null, null,
                cursor, template.startTime(), template.endTime(), template.status(),
                template.capacity(), template.assignments(), template.notes(), template.savedKg()
            );
            created.add(create(occurrence));
            cursor = cursor.plusWeeks(1);
        }
        return created;
    }

    public Optional<Pickup> update(Long id, Pickup pickup) {
        return repository.findById(id).map(entity -> {
            validate(pickup);
            PartnerEntity partner = null;
            EventEntity event = null;
            if (pickup.eventId() != null) {
                event = loadEvent(pickup.eventId());
            } else {
                partner = loadPartner(pickup.partnerId());
            }
            Pickup.Status oldStatus = entity.getStatus();
            PickupMapper.applyToEntity(entity, pickup, partner, event);
            PickupEntity saved = repository.save(entity);
            Pickup.Status newStatus = saved.getStatus();
            if (oldStatus != newStatus) {
                Long actorId = currentUserIdOrNull();
                eventPublisher.publishEvent(new PickupStatusChangedEvent(
                    saved.getId(), oldStatus, newStatus, actorId));
            }
            return PickupMapper.toDto(saved, resolveUsers(List.of(saved)));
        });
    }

    private static Long currentUserIdOrNull() {
        try {
            return CurrentUser.requireId();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private List<Pickup> mapAll(List<PickupEntity> entities) {
        Map<Long, User> users = resolveUsers(entities);
        return entities.stream().map(e -> PickupMapper.toDto(e, users)).toList();
    }

    private Map<Long, User> resolveUsers(List<PickupEntity> entities) {
        Set<Long> ids = new HashSet<>();
        for (PickupEntity e : entities) {
            if (e.getAssignments() == null) continue;
            for (PickupEntity.AssignmentEmbeddable a : e.getAssignments()) {
                if (a.getUserId() != null) ids.add(a.getUserId());
            }
        }
        return ids.stream()
            .map(userService::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(User::id, u -> u));
    }

    private PartnerEntity loadPartner(Long partnerId) {
        if (partnerId == null) {
            throw new IllegalArgumentException("partnerId is required");
        }
        return partnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("partner not found: " + partnerId));
    }

    private EventEntity loadEvent(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("event not found: " + eventId));
    }

    private void validate(Pickup pickup) {
        if ((pickup.partnerId() == null) == (pickup.eventId() == null)) {
            throw new IllegalArgumentException("exactly one of partnerId or eventId must be set");
        }
        if (pickup.date() == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (pickup.startTime() == null || !TIME.matcher(pickup.startTime()).matches()) {
            throw new IllegalArgumentException("startTime must match HH:mm");
        }
        if (pickup.endTime() == null || !TIME.matcher(pickup.endTime()).matches()) {
            throw new IllegalArgumentException("endTime must match HH:mm");
        }
        if (pickup.endTime().compareTo(pickup.startTime()) <= 0) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (pickup.capacity() <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        int assigned = pickup.assignments() == null ? 0 : pickup.assignments().size();
        if (assigned > pickup.capacity()) {
            throw new IllegalArgumentException("capacity must be >= assignments count");
        }
    }
}
