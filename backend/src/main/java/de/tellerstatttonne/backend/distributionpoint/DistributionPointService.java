package de.tellerstatttonne.backend.distributionpoint;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DistributionPointService {

    private final DistributionPointRepository repository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DistributionPointService(
        DistributionPointRepository repository,
        UserRepository userRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<DistributionPoint> findAll() {
        return repository.findAll().stream()
            .sorted(Comparator.comparing(DistributionPointEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(DistributionPointMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DistributionPoint> findById(Long id) {
        return repository.findById(id).map(DistributionPointMapper::toDto);
    }

    public DistributionPoint create(DistributionPoint dto) {
        validate(dto);
        DistributionPointEntity entity = new DistributionPointEntity();
        DistributionPointMapper.applyScalarFields(entity, dto);
        entity.setOperators(resolveOperators(dto.operators()));
        DistributionPointEntity saved = repository.save(entity);

        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.DISTRIBUTION_POINT_CREATED)
            .actorUserId(currentActorId())
            .target("DISTRIBUTION_POINT", saved.getId())
            .message("Verteilerplatz angelegt: " + saved.getName())
            .build());

        return DistributionPointMapper.toDto(saved);
    }

    public Optional<DistributionPoint> update(Long id, DistributionPoint dto) {
        return repository.findById(id).map(entity -> {
            validate(dto);
            DistributionPointMapper.applyScalarFields(entity, dto);
            entity.setOperators(resolveOperators(dto.operators()));
            DistributionPointEntity saved = repository.save(entity);

            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.DISTRIBUTION_POINT_UPDATED)
                .actorUserId(currentActorId())
                .target("DISTRIBUTION_POINT", saved.getId())
                .message("Verteilerplatz aktualisiert: " + saved.getName())
                .build());

            return DistributionPointMapper.toDto(saved);
        });
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(entity -> {
            String name = entity.getName();
            repository.delete(entity);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.DISTRIBUTION_POINT_DELETED)
                .actorUserId(currentActorId())
                .target("DISTRIBUTION_POINT", id)
                .message("Verteilerplatz gelöscht: " + name)
                .build());
            return true;
        }).orElse(false);
    }

    private Set<UserEntity> resolveOperators(List<DistributionPoint.OperatorRef> refs) {
        if (refs == null || refs.isEmpty()) return new HashSet<>();
        List<Long> ids = new ArrayList<>();
        for (DistributionPoint.OperatorRef ref : refs) {
            if (ref != null && ref.id() != null) ids.add(ref.id());
        }
        if (ids.isEmpty()) return new HashSet<>();
        List<UserEntity> users = userRepository.findAllById(ids);
        if (users.size() != ids.stream().distinct().count()) {
            throw new IllegalArgumentException("Unbekannter Betreiber in operators");
        }
        return new HashSet<>(users);
    }

    private void validate(DistributionPoint dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("name ist erforderlich");
        }
        if (dto.openingSlots() != null) {
            for (DistributionPoint.OpeningSlot slot : dto.openingSlots()) {
                if (slot.weekday() == null) {
                    throw new IllegalArgumentException("Öffnungszeit: Wochentag fehlt");
                }
                if (slot.startTime() == null || slot.startTime().isBlank()
                    || slot.endTime() == null || slot.endTime().isBlank()) {
                    throw new IllegalArgumentException("Öffnungszeit: Start- und Endzeit erforderlich");
                }
            }
        }
    }

    private static Long currentActorId() {
        try {
            return CurrentUser.requireId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
