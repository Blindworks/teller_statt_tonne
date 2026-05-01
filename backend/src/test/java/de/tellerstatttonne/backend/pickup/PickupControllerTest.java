package de.tellerstatttonne.backend.pickup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerService;
import de.tellerstatttonne.backend.user.Role;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PickupControllerTest {

    @Autowired private PickupController controller;
    @Autowired private PickupRepository pickupRepository;
    @Autowired private PartnerService partnerService;
    @Autowired private UserRepository userRepository;

    private Long partnerId;
    private Long memberId;

    @BeforeEach
    void setup() {
        pickupRepository.deleteAll();
        Partner partner = partnerService.create(new Partner(
            null, "Bio-Markt Sonne", Partner.Category.SUPERMARKET,
            "Hauptstraße 1", "10115", "Berlin", null,
            new Partner.Contact("Ina", "ina@example.de", "+49 30 111"),
            List.of(), Partner.Status.ACTIVE, null, null
        ));
        partnerId = partner.id();

        UserEntity user = new UserEntity();
        user.setEmail("lisa-" + System.nanoTime() + "@example.de");
        user.setPasswordHash("dummy");
        user.setRole(Role.RETTER);
        user.setFirstName("Lisa");
        user.setLastName("Muster");
        user.setPhone("+49 30 222");
        user.setCity("Berlin");
        user.setOnlineStatus(UserEntity.OnlineStatus.ONLINE);
        user.setStatus(UserEntity.Status.ACTIVE);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        memberId = userRepository.save(user).getId();

        UserEntity admin = new UserEntity();
        admin.setEmail("admin-" + System.nanoTime() + "@example.de");
        admin.setPasswordHash("dummy");
        admin.setRole(Role.ADMINISTRATOR);
        admin.setFirstName("Ada");
        admin.setLastName("Admin");
        admin.setOnlineStatus(UserEntity.OnlineStatus.ONLINE);
        admin.setStatus(UserEntity.Status.ACTIVE);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        Long adminId = userRepository.save(admin).getId();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                String.valueOf(adminId), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))
            )
        );
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createListUpdateDeleteRoundTrip() {
        Pickup payload = new Pickup(
            null, partnerId, null, null, null, null, null,
            LocalDate.of(2026, 4, 21), "19:30", "20:00",
            Pickup.Status.SCHEDULED, 2,
            List.of(new Pickup.Assignment(memberId, null, null)),
            "Bitte Rückseite benutzen"
        );

        Pickup created = controller.create(payload).getBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.partnerName()).isEqualTo("Bio-Markt Sonne");
        assertThat(created.partnerCategory()).isEqualTo(Partner.Category.SUPERMARKET);
        assertThat(created.assignments()).hasSize(1);
        assertThat(created.assignments().get(0).memberName()).isEqualTo("Lisa Muster");

        List<Pickup> week = controller.list(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));
        assertThat(week).hasSize(1);

        List<Pickup> recent = controller.recent();
        assertThat(recent).hasSize(1);

        Pickup updatedPayload = new Pickup(
            created.id(), partnerId, null, null, null, null, null,
            created.date(), "19:30", "20:00",
            Pickup.Status.COMPLETED, 2, created.assignments(), "done"
        );
        Pickup updated = controller.update(created.id(), updatedPayload).getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.status()).isEqualTo(Pickup.Status.COMPLETED);
        assertThat(updated.notes()).isEqualTo("done");

        assertThat(controller.delete(created.id()).getStatusCode().value()).isEqualTo(204);
        assertThat(controller.list(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26))).isEmpty();
    }

    @Test
    void rejectsEndBeforeStart() {
        Pickup bad = new Pickup(
            null, partnerId, null, null, null, null, null,
            LocalDate.of(2026, 4, 21), "20:00", "19:00",
            Pickup.Status.SCHEDULED, 1, List.of(), null
        );
        assertThatThrownBy(() -> controller.create(bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCapacityBelowAssignments() {
        Pickup bad = new Pickup(
            null, partnerId, null, null, null, null, null,
            LocalDate.of(2026, 4, 21), "10:00", "10:30",
            Pickup.Status.SCHEDULED, 1,
            List.of(
                new Pickup.Assignment(memberId, null, null),
                new Pickup.Assignment(memberId, null, null)
            ),
            null
        );
        assertThatThrownBy(() -> controller.create(bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMissingReturns404() {
        Pickup payload = new Pickup(
            -1L, partnerId, null, null, null, null, null,
            LocalDate.of(2026, 4, 21), "10:00", "10:30",
            Pickup.Status.SCHEDULED, 1, List.of(), null
        );
        assertThat(controller.update(-1L, payload).getStatusCode().value()).isEqualTo(404);
    }
}
