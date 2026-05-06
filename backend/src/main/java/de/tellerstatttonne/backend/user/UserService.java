package de.tellerstatttonne.backend.user;

import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRoles(roles);
        entity.setFirstName(request.firstName().trim());
        entity.setLastName(request.lastName().trim());
        entity.setPhone(request.phone());
        entity.setStreet(request.street());
        entity.setPostalCode(request.postalCode());
        entity.setCity(request.city());
        entity.setCountry(request.country());
        return UserMapper.toDto(repository.save(entity));
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
        return repository.findAll(spec).stream().map(UserMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserMapper::toDto);
    }

    public Optional<User> update(Long id, User user) {
        return repository.findById(id).map(entity -> {
            validateProfile(user);
            UserMapper.applyProfileToEntity(entity, user);
            entity.setRoles(resolveRoles(new HashSet<>(user.roles())));
            return UserMapper.toDto(repository.save(entity));
        });
    }

    public Optional<User> updatePhotoUrl(Long id, String photoUrl) {
        return repository.findById(id).map(entity -> {
            entity.setPhotoUrl(photoUrl);
            return UserMapper.toDto(repository.save(entity));
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> findPhotoUrl(Long id) {
        return repository.findById(id).map(UserEntity::getPhotoUrl);
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
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
