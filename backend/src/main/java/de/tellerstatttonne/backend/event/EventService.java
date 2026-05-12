package de.tellerstatttonne.backend.event;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.partner.GeocodingService;
import de.tellerstatttonne.backend.partner.GeocodingService.Coordinates;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService {

    public enum Scope { ACTIVE, PAST, ALL }

    private final EventRepository repository;
    private final GeocodingService geocodingService;
    private final ApplicationEventPublisher eventPublisher;

    public EventService(EventRepository repository,
                        GeocodingService geocodingService,
                        ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.geocodingService = geocodingService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Event> findAll(Scope scope) {
        LocalDate today = LocalDate.now();
        List<EventEntity> entities = switch (scope) {
            case ACTIVE -> repository.findByEndDateGreaterThanEqualOrderByStartDateAsc(today);
            case PAST -> repository.findByEndDateLessThanOrderByStartDateDesc(today);
            case ALL -> repository.findAll();
        };
        return entities.stream().map(EventMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return repository.findById(id).map(EventMapper::toDto);
    }

    public Event create(Event dto) {
        validate(dto);
        EventEntity entity = new EventEntity();
        EventMapper.applyScalarFields(entity, dto);
        if (!hasCoordinates(dto)) {
            applyGeocoding(entity);
        }
        EventEntity saved = repository.save(entity);

        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.EVENT_CREATED)
            .actorUserId(currentActorId())
            .target("EVENT", saved.getId())
            .message("Veranstaltung angelegt: " + saved.getName())
            .build());

        return EventMapper.toDto(saved);
    }

    public Optional<Event> update(Long id, Event dto) {
        return repository.findById(id).map(entity -> {
            validate(dto);
            boolean addressChanged =
                !equal(entity.getStreet(), dto.street())
                || !equal(entity.getPostalCode(), dto.postalCode())
                || !equal(entity.getCity(), dto.city());

            EventMapper.applyScalarFields(entity, dto);
            if (!hasCoordinates(dto)
                && (addressChanged || entity.getLatitude() == null || entity.getLongitude() == null)) {
                applyGeocoding(entity);
            }
            EventEntity saved = repository.save(entity);

            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.EVENT_UPDATED)
                .actorUserId(currentActorId())
                .target("EVENT", saved.getId())
                .message("Veranstaltung aktualisiert: " + saved.getName())
                .build());

            return EventMapper.toDto(saved);
        });
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(entity -> {
            String name = entity.getName();
            repository.delete(entity);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.EVENT_DELETED)
                .actorUserId(currentActorId())
                .target("EVENT", id)
                .message("Veranstaltung gelöscht: " + name)
                .build());
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<String> findLogoUrl(Long id) {
        return repository.findById(id).map(EventEntity::getLogoUrl);
    }

    public Optional<Event> updateLogoUrl(Long id, String logoUrl) {
        return repository.findById(id).map(entity -> {
            entity.setLogoUrl(logoUrl);
            return EventMapper.toDto(repository.save(entity));
        });
    }

    private void applyGeocoding(EventEntity entity) {
        Optional<Coordinates> coords = geocodingService.geocode(
            entity.getStreet(), entity.getPostalCode(), entity.getCity());
        coords.ifPresent(c -> {
            entity.setLatitude(c.lat());
            entity.setLongitude(c.lon());
        });
    }

    private void validate(Event dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("name ist erforderlich");
        }
        if (dto.startDate() == null) {
            throw new IllegalArgumentException("startDate ist erforderlich");
        }
        if (dto.endDate() == null) {
            throw new IllegalArgumentException("endDate ist erforderlich");
        }
        if (dto.endDate().isBefore(dto.startDate())) {
            throw new IllegalArgumentException("endDate darf nicht vor startDate liegen");
        }
        boolean hasCity = dto.city() != null && !dto.city().isBlank();
        if (!hasCity && !hasCoordinates(dto)) {
            throw new IllegalArgumentException(
                "Lokalität erforderlich: Adresse oder Markierung auf der Karte");
        }
    }

    private static boolean hasCoordinates(Event dto) {
        return dto.latitude() != null && dto.longitude() != null;
    }

    private static boolean equal(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    private static Long currentActorId() {
        try {
            return CurrentUser.requireId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
