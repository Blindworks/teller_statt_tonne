package de.tellerstatttonne.backend.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoleServiceTest {

    @Autowired private RoleService service;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    void seededRolesAreListed() {
        List<Role> roles = service.list(false);
        assertThat(roles).extracting(Role::name)
            .contains("ADMINISTRATOR", "BOTSCHAFTER", "RETTER", "NEW_MEMBER");
    }

    @Test
    void createRequiresUpperSnakeCaseInService() {
        Role created = service.create(new RoleCreateRequest(
            "TEST_ROLE", "Testrolle", "Beschreibung", 50, true));
        assertThat(created.name()).isEqualTo("TEST_ROLE");
        assertThat(roleRepository.existsByName("TEST_ROLE")).isTrue();
    }

    @Test
    void createRejectsDuplicateName() {
        service.create(new RoleCreateRequest("DUP_ROLE", "Dup", null, 50, true));
        assertThatThrownBy(() -> service.create(
            new RoleCreateRequest("DUP_ROLE", "Other", null, 50, true)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteAdminRoleIsRejected() {
        Long adminRoleId = roleRepository.findByName("ADMINISTRATOR").orElseThrow().getId();
        assertThatThrownBy(() -> service.delete(adminRoleId))
            .isInstanceOf(RoleService.ConflictException.class);
    }

    @Test
    void deleteCustomRoleDetachesFromUsers() {
        Role custom = service.create(new RoleCreateRequest(
            "TEMP_ROLE", "Temp", null, 99, true));

        UserEntity user = new UserEntity();
        user.setEmail("temp-" + System.nanoTime() + "@example.de");
        user.setPasswordHash("dummy");
        user.setFirstName("Temp");
        user.setLastName("User");
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(roleRepository.findByName("RETTER").orElseThrow());
        roles.add(roleRepository.findById(custom.id()).orElseThrow());
        user.setRoles(roles);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        Long userId = userRepository.save(user).getId();

        service.delete(custom.id());

        assertThat(roleRepository.existsByName("TEMP_ROLE")).isFalse();
        UserEntity reloaded = userRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getRoleNames()).doesNotContain("TEMP_ROLE").contains("RETTER");
    }
}
