package de.tellerstatttonne.backend.user;

import de.tellerstatttonne.backend.auth.passwordreset.PasswordResetService;
import de.tellerstatttonne.backend.hygiene.HygieneCertificateRepository;
import de.tellerstatttonne.backend.hygiene.HygieneCertificateStatus;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final HygieneCertificateRepository hygieneRepository;
    private final PasswordResetService passwordResetService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository repository, RoleRepository roleRepository,
                       HygieneCertificateRepository hygieneRepository,
                       PasswordResetService passwordResetService,
                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.hygieneRepository = hygieneRepository;
        this.passwordResetService = passwordResetService;
        this.eventPublisher = eventPublisher;
    }

    private static Long currentActorId() {
        try {
            return de.tellerstatttonne.backend.auth.CurrentUser.requireId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public User adminCreate(AdminCreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (request.firstName() == null || request.firstName().isBlank()) {
            throw new IllegalArgumentException("firstName is required");
        }
        if (request.lastName() == null || request.lastName().isBlank()) {
            throw new IllegalArgumentException("lastName is required");
        }
        if (request.roleNames() == null || request.roleNames().isEmpty()) {
            throw new IllegalArgumentException("at least one role is required");
        }
        Set<RoleEntity> roles = resolveRoles(request.roleNames());
        UserEntity entity = new UserEntity();
        entity.setEmail(email);
        entity.setRoles(roles);
        entity.setFirstName(request.firstName().trim());
        entity.setLastName(request.lastName().trim());
        entity.setPhone(request.phone());
        entity.setStreet(request.street());
        entity.setPostalCode(request.postalCode());
        entity.setCity(request.city());
        entity.setCountry(request.country());
        entity.setStatus(UserEntity.Status.PENDING);
        UserEntity saved = repository.save(entity);
        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.USER_CREATED)
            .actorUserId(currentActorId())
            .target("USER", saved.getId())
            .message("Nutzer angelegt: " + saved.getEmail() + " (Rollen: "
                + String.join(",", saved.getRoleNames()) + ")")
            .build());
        passwordResetService.sendInvitation(saved);
        return toDto(saved);
    }

    public User resendInvitation(Long id) {
        UserEntity entity = requireUser(id);
        if (entity.getPasswordHash() != null) {
            throw new IllegalStateException(
                "Einladung kann nicht erneut gesendet werden: Nutzer hat bereits ein Passwort");
        }
        passwordResetService.sendInvitation(entity);
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<User> findAll(String roleName, boolean activeOnly, String search) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (roleName != null && !roleName.isBlank()) {
                Join<UserEntity, RoleEntity> rolesJoin = root.join("roles");
                predicates.add(cb.equal(rolesJoin.get("name"), roleName));
                if (query != null) {
                    query.distinct(true);
                }
            }
            if (activeOnly) {
                predicates.add(cb.equal(root.get("onlineStatus"), UserEntity.OnlineStatus.ONLINE));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    public Optional<User> update(Long id, User user) {
        return repository.findById(id).map(entity -> {
            validateProfile(user);
            Set<String> oldRoles = new TreeSet<>(entity.getRoleNames());
            UserMapper.applyProfileToEntity(entity, user);
            entity.setRoles(resolveRoles(new HashSet<>(user.roles())));
            UserEntity saved = repository.save(entity);
            Set<String> newRoles = new TreeSet<>(saved.getRoleNames());
            if (!oldRoles.equals(newRoles)) {
                eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.USER_ROLES_CHANGED)
                    .actorUserId(currentActorId())
                    .target("USER", saved.getId())
                    .message("Rollen geaendert fuer " + saved.getEmail() + ": "
                        + oldRoles.stream().collect(Collectors.joining(",")) + " -> "
                        + newRoles.stream().collect(Collectors.joining(",")))
                    .build());
            } else {
                eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.USER_UPDATED)
                    .actorUserId(currentActorId())
                    .target("USER", saved.getId())
                    .message("Nutzerprofil aktualisiert: " + saved.getEmail())
                    .build());
            }
            return toDto(saved);
        });
    }

    public Optional<User> updateSelfProfile(Long id, UserController.SelfProfileRequest req) {
        return repository.findById(id).map(entity -> {
            entity.setPhone(req.phone());
            entity.setStreet(req.street());
            entity.setPostalCode(req.postalCode());
            entity.setCity(req.city());
            entity.setCountry(req.country());
            UserEntity saved = repository.save(entity);
            promoteToActiveIfReady(saved.getId());
            return toDto(repository.findById(saved.getId()).orElse(saved));
        });
    }

    public Optional<User> updatePhotoUrl(Long id, String photoUrl) {
        return repository.findById(id).map(entity -> {
            entity.setPhotoUrl(photoUrl);
            return toDto(repository.save(entity));
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> findPhotoUrl(Long id) {
        return repository.findById(id).map(UserEntity::getPhotoUrl);
    }

    public boolean delete(Long id) {
        Optional<UserEntity> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        UserEntity user = opt.get();
        String email = user.getEmail();
        repository.deleteById(id);
        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.USER_DELETED)
            .actorUserId(currentActorId())
            .target("USER", id)
            .message("Nutzer geloescht: " + email)
            .build());
        return true;
    }

    public User markIntroductionCompleted(Long id) {
        UserEntity entity = requireUser(id);
        if (entity.getIntroductionCompletedAt() != null) {
            return toDto(entity);
        }
        if (entity.getStatus() == UserEntity.Status.LEFT
            || entity.getStatus() == UserEntity.Status.REMOVED) {
            throw new IllegalStateException(
                "Einfuehrung kann nicht bestaetigt werden im Status " + entity.getStatus());
        }
        entity.setIntroductionCompletedAt(Instant.now());
        UserEntity.Status before = entity.getStatus();
        promoteToActiveIfReady(entity);
        UserEntity saved = repository.save(entity);
        logStatusEvent(saved, before, "Einfuehrungsgespraech bestaetigt");
        return toDto(saved);
    }

    /**
     * Re-evaluates the onboarding gate. Called from this service and from
     * {@link de.tellerstatttonne.backend.hygiene.HygieneCertificateService} after
     * a hygiene certificate has been approved. Promotes a {@code PENDING} user
     * to {@code ACTIVE} once both onboarding requirements are satisfied.
     */
    public Optional<User> promoteToActiveIfReady(Long id) {
        return repository.findById(id).map(entity -> {
            UserEntity.Status before = entity.getStatus();
            if (promoteToActiveIfReady(entity)) {
                UserEntity saved = repository.save(entity);
                logStatusEvent(saved, before, "Onboarding abgeschlossen");
                return toDto(saved);
            }
            return toDto(entity);
        });
    }

    private boolean promoteToActiveIfReady(UserEntity entity) {
        if (entity.getStatus() != UserEntity.Status.PENDING) return false;
        if (entity.getIntroductionCompletedAt() == null) return false;
        if (!hasApprovedHygieneCertificate(entity.getId())) return false;
        if (entity.getAgreementUploadedAt() == null) return false;
        if (entity.getTestPickupCompletedAt() == null) return false;
        if (!isProfileComplete(entity)) return false;
        entity.setStatus(UserEntity.Status.ACTIVE);
        return true;
    }

    private static boolean isProfileComplete(UserEntity user) {
        return notBlank(user.getPhone())
            && notBlank(user.getStreet())
            && notBlank(user.getPostalCode())
            && notBlank(user.getCity());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public User pause(Long id) {
        return changeStatus(id, UserEntity.Status.PAUSED,
            EnumSet.of(UserEntity.Status.ACTIVE), "pausiert");
    }

    public User reactivate(Long id) {
        return changeStatus(id, UserEntity.Status.ACTIVE,
            EnumSet.of(UserEntity.Status.PAUSED), "reaktiviert");
    }

    public User markLeft(Long id) {
        return changeStatus(id, UserEntity.Status.LEFT,
            EnumSet.of(UserEntity.Status.PENDING, UserEntity.Status.ACTIVE, UserEntity.Status.PAUSED),
            "ausgetreten");
    }

    public User remove(Long id) {
        return changeStatus(id, UserEntity.Status.REMOVED,
            EnumSet.of(UserEntity.Status.PENDING, UserEntity.Status.ACTIVE, UserEntity.Status.PAUSED),
            "entfernt");
    }

    private User changeStatus(Long id, UserEntity.Status target,
                              Set<UserEntity.Status> allowedFrom, String description) {
        UserEntity entity = requireUser(id);
        UserEntity.Status before = entity.getStatus();
        if (!allowedFrom.contains(before)) {
            throw new IllegalStateException(
                "Statuswechsel " + before + " -> " + target + " nicht erlaubt");
        }
        entity.setStatus(target);
        UserEntity saved = repository.save(entity);
        logStatusEvent(saved, before, description);
        return toDto(saved);
    }

    private void logStatusEvent(UserEntity user, UserEntity.Status before, String description) {
        if (before == user.getStatus()) return;
        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.USER_STATUS_CHANGED)
            .actorUserId(currentActorId())
            .target("USER", user.getId())
            .message("Nutzer " + user.getEmail() + " " + description + " ("
                + before + " -> " + user.getStatus() + ")")
            .build());
    }

    private UserEntity requireUser(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + id));
    }

    private boolean hasApprovedHygieneCertificate(Long userId) {
        return hygieneRepository.findByUserId(userId)
            .map(c -> c.getStatus() == HygieneCertificateStatus.APPROVED)
            .orElse(false);
    }

    private User toDto(UserEntity entity) {
        return UserMapper.toDto(entity, hasApprovedHygieneCertificate(entity.getId()));
    }

    private void validateProfile(User user) {
        if (user.firstName() == null || user.firstName().isBlank()) {
            throw new IllegalArgumentException("firstName is required");
        }
        if (user.lastName() == null || user.lastName().isBlank()) {
            throw new IllegalArgumentException("lastName is required");
        }
        if (user.roles() == null || user.roles().isEmpty()) {
            throw new IllegalArgumentException("at least one role is required");
        }
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        Set<RoleEntity> resolved = new HashSet<>();
        for (String name : roleNames) {
            if (name == null || name.isBlank()) continue;
            RoleEntity role = roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + name));
            resolved.add(role);
        }
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("at least one role is required");
        }
        return resolved;
    }
}
