package de.tellerstatttonne.backend.appointment;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.role.Role;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository repository;
    private final AppointmentReadRepository readRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository repository,
                              AppointmentReadRepository readRepository,
                              RoleRepository roleRepository,
                              UserRepository userRepository) {
        this.repository = repository;
        this.readRepository = readRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Appointment> listForCurrentUser(boolean upcoming) {
        UserEntity user = currentUser();
        boolean admin = isAdmin(user);
        Set<Long> roleIds = user.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toSet());
        Instant now = Instant.now();
        List<AppointmentEntity> entities;
        if (admin) {
            entities = upcoming ? repository.findUpcomingAll(now) : repository.findPastAll(now);
        } else if (roleIds.isEmpty()) {
            entities = List.of();
        } else {
            entities = upcoming
                ? repository.findUpcomingForRoles(roleIds, now)
                : repository.findPastForRoles(roleIds, now);
        }
        Set<Long> readIds = readAppointmentIds(user.getId(), entities);
        return entities.stream()
            .map(e -> AppointmentMapper.toDto(e, readIds.contains(e.getId()), canEdit(e, user, admin)))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Appointment> findForCurrentUser(Long id) {
        UserEntity user = currentUser();
        boolean admin = isAdmin(user);
        return repository.findById(id)
            .filter(a -> admin || isVisibleTo(a, user))
            .map(a -> {
                boolean read = readRepository
                    .findByAppointmentIdAndUserId(a.getId(), user.getId())
                    .isPresent();
                return AppointmentMapper.toDto(a, read, canEdit(a, user, admin));
            });
    }

    @Transactional(readOnly = true)
    public List<AppointmentDtos.PublicAppointment> listPublicUpcoming() {
        return repository.findPublicUpcoming(Instant.now()).stream()
            .map(AppointmentMapper::toPublicDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCountForCurrentUser() {
        UserEntity user = currentUser();
        boolean admin = isAdmin(user);
        Set<Long> roleIds = user.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toSet());
        List<AppointmentEntity> entities;
        Instant now = Instant.now();
        if (admin) {
            entities = repository.findUpcomingAll(now);
        } else if (roleIds.isEmpty()) {
            return 0;
        } else {
            entities = repository.findUpcomingForRoles(roleIds, now);
        }
        Set<Long> readIds = readAppointmentIds(user.getId(), entities);
        return entities.stream().filter(a -> !readIds.contains(a.getId())).count();
    }

    public Appointment create(AppointmentDtos.AppointmentInput input) {
        UserEntity user = currentUser();
        validate(input);
        AppointmentEntity entity = new AppointmentEntity();
        applyFields(entity, input);
        entity.setCreatedById(user.getId());
        AppointmentEntity saved = repository.save(entity);
        return AppointmentMapper.toDto(saved, false, true);
    }

    public Optional<Appointment> update(Long id, AppointmentDtos.AppointmentInput input) {
        UserEntity user = currentUser();
        boolean admin = isAdmin(user);
        return repository.findById(id).map(entity -> {
            if (!admin && !user.getId().equals(entity.getCreatedById())) {
                throw new AccessDeniedException("Nur Ersteller oder Administrator dürfen den Termin ändern");
            }
            validate(input);
            applyFields(entity, input);
            AppointmentEntity saved = repository.save(entity);
            boolean read = readRepository
                .findByAppointmentIdAndUserId(saved.getId(), user.getId())
                .isPresent();
            return AppointmentMapper.toDto(saved, read, canEdit(saved, user, admin));
        });
    }

    public boolean delete(Long id) {
        UserEntity user = currentUser();
        boolean admin = isAdmin(user);
        return repository.findById(id).map(entity -> {
            if (!admin && !user.getId().equals(entity.getCreatedById())) {
                throw new AccessDeniedException("Nur Ersteller oder Administrator dürfen den Termin löschen");
            }
            readRepository.deleteByAppointmentId(id);
            repository.delete(entity);
            return true;
        }).orElse(false);
    }

    public void markRead(Long appointmentId) {
        UserEntity user = currentUser();
        boolean admin = isAdmin(user);
        AppointmentEntity entity = repository.findById(appointmentId)
            .filter(a -> admin || isVisibleTo(a, user))
            .orElseThrow(() -> new IllegalArgumentException("Termin nicht gefunden"));
        readRepository
            .findByAppointmentIdAndUserId(entity.getId(), user.getId())
            .orElseGet(() -> readRepository.save(
                new AppointmentReadEntity(entity.getId(), user.getId(), Instant.now())));
    }

    private void applyFields(AppointmentEntity entity, AppointmentDtos.AppointmentInput input) {
        entity.setTitle(input.title().trim());
        entity.setDescription(blankToNull(input.description()));
        entity.setStartTime(input.startTime());
        entity.setEndTime(input.endTime());
        entity.setLocation(blankToNull(input.location()));
        entity.setAttachmentUrl(blankToNull(input.attachmentUrl()));
        entity.setPublic(Boolean.TRUE.equals(input.isPublic()));
        Set<RoleEntity> roles = resolveRoles(input.targetRoleIds());
        entity.setTargetRoles(roles);
    }

    private Set<RoleEntity> resolveRoles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        List<RoleEntity> found = roleRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new IllegalArgumentException("Unbekannte Rollen-ID in targetRoleIds");
        }
        return new HashSet<>(found);
    }

    private void validate(AppointmentDtos.AppointmentInput input) {
        if (input.title() == null || input.title().isBlank()) {
            throw new IllegalArgumentException("title ist erforderlich");
        }
        if (input.startTime() == null) {
            throw new IllegalArgumentException("startTime ist erforderlich");
        }
        if (input.endTime() == null) {
            throw new IllegalArgumentException("endTime ist erforderlich");
        }
        if (input.endTime().isBefore(input.startTime())) {
            throw new IllegalArgumentException("endTime darf nicht vor startTime liegen");
        }
        List<Long> roleIds = input.targetRoleIds();
        boolean publicFlag = Boolean.TRUE.equals(input.isPublic());
        if ((roleIds == null || roleIds.isEmpty()) && !publicFlag) {
            throw new IllegalArgumentException(
                "Mindestens eine Ziel-Rolle oder \"öffentlich\" erforderlich");
        }
    }

    private boolean isVisibleTo(AppointmentEntity a, UserEntity user) {
        Set<Long> userRoleIds = user.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toSet());
        return a.getTargetRoles().stream().anyMatch(r -> userRoleIds.contains(r.getId()));
    }

    private boolean canEdit(AppointmentEntity a, UserEntity user, boolean admin) {
        return admin || user.getId().equals(a.getCreatedById());
    }

    private boolean isAdmin(UserEntity user) {
        return user.hasRole(Role.ADMIN_ROLE_NAME);
    }

    private UserEntity currentUser() {
        Long id = CurrentUser.requireId();
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("aktueller Nutzer nicht gefunden"));
    }

    private Set<Long> readAppointmentIds(Long userId, List<AppointmentEntity> entities) {
        if (entities.isEmpty()) return Set.of();
        List<Long> ids = new ArrayList<>(entities.size());
        for (AppointmentEntity e : entities) ids.add(e.getId());
        return readRepository.findByUserIdAndAppointmentIdIn(userId, ids).stream()
            .map(AppointmentReadEntity::getAppointmentId)
            .collect(Collectors.toSet());
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
