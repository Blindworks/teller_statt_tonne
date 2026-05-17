package de.tellerstatttonne.backend.specialpickup;

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
public class SpecialPickupService {

    public enum Scope { ACTIVE, PAST, ALL }

    private final SpecialPickupRepository repository;
    private final GeocodingService geocodingService;
    private final ApplicationEventPublisher eventPublisher;

    public SpecialPickupService(SpecialPickupRepository repository,
                                GeocodingService geocodingService,
                                ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.geocodingService = geocodingService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<SpecialPickup> findAll(Scope scope) {
        LocalDate today = LocalDate.now();
        List<SpecialPickupEntity> entities = switch (scope) {
            case ACTIVE -> repository.findByEndDateGreaterThanEqualOrderByStartDateAsc(today);
            case PAST -> repository.findByEndDateLessThanOrderByStartDateDesc(today);
            case ALL -> repository.findAll();
        };
        return entities.stream().map(SpecialPickupMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<SpecialPickup> findById(Long id) {
        return repository.findById(id).map(SpecialPickupMapper::toDto);
    }

    public SpecialPickup create(SpecialPickup dto) {
        validate(dto);
        SpecialPickupEntity entity = new SpecialPickupEntity();
        SpecialPickupMapper.applyScalarFields(entity, dto);
        if (!hasCoordinates(dto)) {
            applyGeocoding(entity);
        }
        SpecialPickupEntity saved = repository.save(entity);

        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.SPECIAL_PICKUP_CREATED)
            .actorUserId(currentActorId())
            .target("SPECIAL_PICKUP", saved.getId())
            .message("Sonderabholung angelegt: " + saved.getName())
            .build());

        return SpecialPickupMapper.toDto(saved);
    }

    public Optional<SpecialPickup> update(Long id, SpecialPickup dto) {
        return repository.findById(id).map(entity -> {
            validate(dto);
            boolean addressChanged =
                !equal(entity.getStreet(), dto.street())
                || !equal(entity.getPostalCode(), dto.postalCode())
                || !equal(entity.getCity(), dto.city());

            SpecialPickupMapper.applyScalarFields(entity, dto);
            if (!hasCoordinates(dto)
                && (addressChanged || entity.getLatitude() == null || entity.getLongitude() == null)) {
                applyGeocoding(entity);
            }
            SpecialPickupEntity saved = repository.save(entity);

            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.SPECIAL_PICKUP_UPDATED)
                .actorUserId(currentActorId())
                .target("SPECIAL_PICKUP", saved.getId())
                .message("Sonderabholung aktualisiert: " + saved.getName())
                .build());

            return SpecialPickupMapper.toDto(saved);
        });
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(entity -> {
            String name = entity.getName();
            repository.delete(entity);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.SPECIAL_PICKUP_DELETED)
                .actorUserId(currentActorId())
                .target("SPECIAL_PICKUP", id)
                .message("Sonderabholung gelöscht: " + name)
                .build());
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<String> findLogoUrl(Long id) {
        return repository.findById(id).map(SpecialPickupEntity::getLogoUrl);
    }

    public Optional<SpecialPickup> updateLogoUrl(Long id, String logoUrl) {
        return repository.findById(id).map(entity -> {
            entity.setLogoUrl(logoUrl);
            return SpecialPickupMapper.toDto(repository.save(entity));
        });
    }

    private void applyGeocoding(SpecialPickupEntity entity) {
        Optional<Coordinates> coords = geocodingService.geocode(
            entity.getStreet(), entity.getPostalCode(), entity.getCity());
        coords.ifPresent(c -> {
            entity.setLatitude(c.lat());
            entity.setLongitude(c.lon());
        });
    }

    private void validate(SpecialPickup dto) {
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

    private static boolean hasCoordinates(SpecialPickup dto) {
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
