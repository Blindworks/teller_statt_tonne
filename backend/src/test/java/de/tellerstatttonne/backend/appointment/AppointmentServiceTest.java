package de.tellerstatttonne.backend.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AppointmentServiceTest {

    @Autowired private AppointmentService service;
    @Autowired private AppointmentRepository repository;
    @Autowired private AppointmentReadRepository readRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;

    private Long adminId;
    private Long teamleiterId;
    private Long retterId;
    private Long otherRetterId;
    private Long retterRoleId;
    private Long teamleiterRoleId;

    @BeforeEach
    void setup() {
        readRepository.deleteAll();
        repository.deleteAll();

        RoleEntity admin = roleRepository.findByName("ADMINISTRATOR").orElseThrow();
        RoleEntity teamleiter = roleRepository.findByName("TEAMLEITER").orElseThrow();
        RoleEntity retter = roleRepository.findByName("RETTER").orElseThrow();
        retterRoleId = retter.getId();
        teamleiterRoleId = teamleiter.getId();

        adminId = saveUser("admin", Set.of(admin));
        teamleiterId = saveUser("tl", Set.of(teamleiter));
        retterId = saveUser("retter", Set.of(retter));
        otherRetterId = saveUser("retter2", Set.of(retter));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPersistsAppointmentWithTargetRoles() {
        loginAs(teamleiterId, "TEAMLEITER");
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Appointment created = service.create(input("Teamsitzung", start, List.of(retterRoleId), false));

        assertThat(created.id()).isNotNull();
        assertThat(created.title()).isEqualTo("Teamsitzung");
        assertThat(created.targetRoles()).extracting(r -> r.name()).containsExactly("RETTER");
        assertThat(created.canEdit()).isTrue();
        assertThat(created.createdById()).isEqualTo(teamleiterId);
    }

    @Test
    void rejectsEndBeforeStart() {
        loginAs(adminId, "ADMINISTRATOR");
        Instant start = Instant.now();
        AppointmentDtos.AppointmentInput bad = new AppointmentDtos.AppointmentInput(
            "X", null, start, start.minus(1, ChronoUnit.HOURS), null, null, false, List.of(retterRoleId));
        assertThatThrownBy(() -> service.create(bad)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNoRolesAndNotPublic() {
        loginAs(adminId, "ADMINISTRATOR");
        Instant start = Instant.now();
        AppointmentDtos.AppointmentInput bad = new AppointmentDtos.AppointmentInput(
            "X", null, start, start.plusSeconds(60), null, null, false, List.of());
        assertThatThrownBy(() -> service.create(bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ziel-Rolle");
    }

    @Test
    void allowsPublicWithoutRoles() {
        loginAs(adminId, "ADMINISTRATOR");
        Instant start = Instant.now().plusSeconds(3600);
        Appointment created = service.create(new AppointmentDtos.AppointmentInput(
            "Info", null, start, start.plusSeconds(3600), null, null, true, List.of()));
        assertThat(created.isPublic()).isTrue();
        assertThat(created.targetRoles()).isEmpty();
    }

    @Test
    void retterSeesOnlyAppointmentsForOwnRole() {
        loginAs(teamleiterId, "TEAMLEITER");
        Instant start = Instant.now().plusSeconds(3600);
        service.create(input("Für Retter", start, List.of(retterRoleId), false));
        service.create(input("Für Teamleiter", start, List.of(teamleiterRoleId), false));

        loginAs(retterId, "RETTER");
        List<Appointment> visible = service.listForCurrentUser(true);
        assertThat(visible).extracting(Appointment::title).containsExactly("Für Retter");
        assertThat(visible.get(0).canEdit()).isFalse();
    }

    @Test
    void adminSeesAllAndCanEditAll() {
        loginAs(teamleiterId, "TEAMLEITER");
        Appointment created = service.create(
            input("Für Retter", Instant.now().plusSeconds(3600), List.of(retterRoleId), false));

        loginAs(adminId, "ADMINISTRATOR");
        Appointment fetched = service.findForCurrentUser(created.id()).orElseThrow();
        assertThat(fetched.canEdit()).isTrue();
    }

    @Test
    void publicListReturnsOnlyPublicUpcoming() {
        loginAs(adminId, "ADMINISTRATOR");
        Instant future = Instant.now().plusSeconds(3600);
        service.create(input("Privat", future, List.of(retterRoleId), false));
        service.create(new AppointmentDtos.AppointmentInput(
            "Öffentlich", null, future, future.plusSeconds(3600), "Berlin", null, true, List.of()));

        SecurityContextHolder.clearContext();
        List<AppointmentDtos.PublicAppointment> publicList = service.listPublicUpcoming();
        assertThat(publicList).extracting(AppointmentDtos.PublicAppointment::title).containsExactly("Öffentlich");
    }

    @Test
    void teamleiterCannotEditOthersAppointment() {
        loginAs(adminId, "ADMINISTRATOR");
        Appointment created = service.create(
            input("Admin-Termin", Instant.now().plusSeconds(3600), List.of(retterRoleId), false));

        Long otherTl = saveUser("tl2", Set.of(roleRepository.findByName("TEAMLEITER").orElseThrow()));
        loginAs(otherTl, "TEAMLEITER");
        assertThatThrownBy(() -> service.update(created.id(),
            input("hacked", Instant.now().plusSeconds(3600), List.of(retterRoleId), false)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unreadCountReflectsReadMarks() {
        loginAs(teamleiterId, "TEAMLEITER");
        Appointment a = service.create(input("A", Instant.now().plusSeconds(3600), List.of(retterRoleId), false));
        service.create(input("B", Instant.now().plusSeconds(3600), List.of(retterRoleId), false));

        loginAs(retterId, "RETTER");
        assertThat(service.unreadCountForCurrentUser()).isEqualTo(2);
        service.markRead(a.id());
        assertThat(service.unreadCountForCurrentUser()).isEqualTo(1);
    }

    @Test
    void otherRetterStartsWithFullUnread() {
        loginAs(teamleiterId, "TEAMLEITER");
        service.create(input("A", Instant.now().plusSeconds(3600), List.of(retterRoleId), false));

        loginAs(otherRetterId, "RETTER");
        assertThat(service.unreadCountForCurrentUser()).isEqualTo(1);
    }

    private AppointmentDtos.AppointmentInput input(String title, Instant start, List<Long> roleIds, boolean isPublic) {
        return new AppointmentDtos.AppointmentInput(
            title, "desc", start, start.plusSeconds(3600), "Berlin", null, isPublic, roleIds);
    }

    private Long saveUser(String prefix, Set<RoleEntity> roles) {
        UserEntity u = new UserEntity();
        u.setEmail(prefix + "-" + System.nanoTime() + "@example.de");
        u.setPasswordHash("dummy");
        u.setRoles(roles);
        u.setFirstName(prefix);
        u.setLastName("Test");
        u.setOnlineStatus(UserEntity.OnlineStatus.OFFLINE);
        u.setStatus(UserEntity.Status.ACTIVE);
        Instant now = Instant.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return userRepository.save(u).getId();
    }

    private void loginAs(Long userId, String... roles) {
        List<SimpleGrantedAuthority> auths = java.util.Arrays.stream(roles)
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, auths));
    }
}
