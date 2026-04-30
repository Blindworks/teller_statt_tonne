package de.tellerstatttonne.backend.user;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
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
        if (request.role() == null) {
            throw new IllegalArgumentException("role is required");
        }
        UserEntity entity = new UserEntity();
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRole(request.role());
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
    public List<User> findAll(Role role, boolean activeOnly, String search) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
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
        if (user.role() == null) {
            throw new IllegalArgumentException("role is required");
        }
    }
}
