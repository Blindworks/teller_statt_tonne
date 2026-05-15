package de.tellerstatttonne.backend.admin.testuser;

import de.tellerstatttonne.backend.admin.testuser.TestUserDtos.CreateTestUserRequest;
import de.tellerstatttonne.backend.admin.testuser.TestUserDtos.TestUserDto;
import de.tellerstatttonne.backend.auth.AuthDtos.AuthResponse;
import de.tellerstatttonne.backend.auth.AuthService;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TestUserService {

    private static final String ROLE_RETTER = "RETTER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final SecureRandom random = new SecureRandom();

    public TestUserService(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthService authService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public TestUserDto create(CreateTestUserRequest request) {
        String firstName = blankOr(request == null ? null : request.firstName(), "Test");
        String lastName = blankOr(request == null ? null : request.lastName(), "Retter");
        String email = generateUniqueEmail();
        RoleEntity retterRole = roleRepository.findByName(ROLE_RETTER)
            .orElseThrow(() -> new IllegalStateException("Rolle RETTER fehlt"));

        UserEntity entity = new UserEntity();
        entity.setEmail(email);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(retterRole);
        entity.setRoles(roles);
        entity.setStatus(UserEntity.Status.PENDING);
        entity.setTestUser(true);
        entity.setPasswordHash(passwordEncoder.encode(randomSecret()));

        UserEntity saved = userRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TestUserDto> list() {
        return userRepository.findAll().stream()
            .filter(UserEntity::isTestUser)
            .map(this::toDto)
            .toList();
    }

    public void delete(Long id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + id));
        ensureTestUser(user);
        userRepository.deleteById(id);
    }

    public AuthResponse impersonate(Long id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + id));
        ensureTestUser(user);
        return authService.issueTokens(user);
    }

    private void ensureTestUser(UserEntity user) {
        if (!user.isTestUser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Aktion nur fuer Test-User erlaubt");
        }
    }

    private String generateUniqueEmail() {
        for (int i = 0; i < 10; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String email = "test+" + suffix + "@tellerstatttonne.local";
            if (!userRepository.existsByEmail(email)) {
                return email;
            }
        }
        throw new IllegalStateException("Konnte keine eindeutige Test-E-Mail erzeugen");
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private TestUserDto toDto(UserEntity user) {
        return new TestUserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus() == null ? null : user.getStatus().name(),
            user.getCreatedAt()
        );
    }
}
