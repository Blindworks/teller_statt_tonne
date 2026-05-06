package de.tellerstatttonne.backend.role;

import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleService {

    private final RoleRepository repository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> list(boolean includeDisabled) {
        Map<Long, Long> counts = userCountsByRoleId();
        return repository.findAll().stream()
            .filter(r -> includeDisabled || r.isEnabled())
            .sorted(Comparator
                .comparing(RoleEntity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RoleEntity::getName))
            .map(r -> RoleMapper.toDto(r, counts.getOrDefault(r.getId(), 0L)))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Role> get(Long id) {
        Map<Long, Long> counts = userCountsByRoleId();
        return repository.findById(id)
            .map(r -> RoleMapper.toDto(r, counts.getOrDefault(r.getId(), 0L)));
    }

    public Role create(RoleCreateRequest request) {
        if (repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role with name '" + request.name() + "' already exists");
        }
        RoleEntity entity = new RoleEntity();
        entity.setName(request.name().trim());
        entity.setLabel(request.label().trim());
        entity.setDescription(request.description());
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 100);
        entity.setEnabled(request.enabled() == null || request.enabled());
        return RoleMapper.toDto(repository.save(entity), 0L);
    }

    public Role update(Long id, RoleUpdateRequest request) {
        RoleEntity entity = repository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Role not found"));

        boolean nameChanged = !entity.getName().equals(request.name());
        if (nameChanged && repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role with name '" + request.name() + "' already exists");
        }
        boolean disabling = entity.isEnabled() && (request.enabled() != null && !request.enabled());
        if (disabling && Role.ADMIN_ROLE_NAME.equals(entity.getName())) {
            throw new ConflictException(
                "Die Administrator-Rolle kann nicht deaktiviert werden.");
        }

        entity.setName(request.name().trim());
        entity.setLabel(request.label().trim());
        entity.setDescription(request.description());
        if (request.sortOrder() != null) entity.setSortOrder(request.sortOrder());
        if (request.enabled() != null) entity.setEnabled(request.enabled());
        return RoleMapper.toDto(repository.save(entity),
            userCountsByRoleId().getOrDefault(entity.getId(), 0L));
    }

    public void delete(Long id) {
        RoleEntity entity = repository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Role not found"));

        if (Role.ADMIN_ROLE_NAME.equals(entity.getName())) {
            throw new ConflictException(
                "Die Administrator-Rolle kann nicht gelöscht werden.");
        }

        for (UserEntity user : userRepository.findAll()) {
            if (user.getRoles().removeIf(r -> r.getId().equals(id))) {
                userRepository.save(user);
            }
        }
        repository.delete(entity);
    }

    private Map<Long, Long> userCountsByRoleId() {
        Map<Long, Long> result = new HashMap<>();
        for (UserEntity u : userRepository.findAll()) {
            for (RoleEntity r : u.getRoles()) {
                result.merge(r.getId(), 1L, Long::sum);
            }
        }
        return result;
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }
}
