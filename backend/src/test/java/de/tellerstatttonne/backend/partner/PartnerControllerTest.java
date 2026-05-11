package de.tellerstatttonne.backend.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.partner.note.PartnerNote;
import de.tellerstatttonne.backend.partner.note.PartnerNoteController;
import de.tellerstatttonne.backend.partner.note.Visibility;
import de.tellerstatttonne.backend.partnercategory.PartnerCategoryRepository;
import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.pickup.PickupService;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
class PartnerControllerTest {

    @Autowired
    private PartnerController controller;

    @Autowired
    private PartnerRepository repository;

    @Autowired
    private PickupRepository pickupRepository;

    @Autowired
    private PickupService pickupService;

    @Autowired
    private PartnerNoteController noteController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PartnerCategoryRepository partnerCategoryRepository;

    private Long adminId;
    private Long bakeryId;
    private Long cafeId;
    private Long butcherId;

    @BeforeEach
    void cleanSlate() {
        pickupRepository.deleteAll();
        repository.deleteAll();

        bakeryId = partnerCategoryRepository.findByCodeIgnoreCase("BAKERY").orElseThrow().getId();
        cafeId = partnerCategoryRepository.findByCodeIgnoreCase("CAFE").orElseThrow().getId();
        butcherId = partnerCategoryRepository.findByCodeIgnoreCase("BUTCHER").orElseThrow().getId();

        UserEntity admin = new UserEntity();
        admin.setEmail("admin-" + System.nanoTime() + "@example.de");
        admin.setPasswordHash("dummy");
        admin.setRoles(Set.of(roleRepository.findByName("ADMINISTRATOR").orElseThrow()));
        admin.setFirstName("Ada");
        admin.setLastName("Admin");
        admin.setOnlineStatus(UserEntity.OnlineStatus.ONLINE);
        admin.setStatus(UserEntity.Status.ACTIVE);
        Instant now = Instant.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        adminId = userRepository.save(admin).getId();

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
    void createFetchUpdateRoundTrip() {
        Partner payload = new Partner(
            null,
            "Bäckerei Sonnenschein",
            bakeryId,
            "Hauptstraße 42",
            "10115",
            "Berlin",
            null,
            new Partner.Contact("Maren Müller", "maren@example.de", "+49 176 1234567"),
            List.of(new Partner.PickupSlot(Partner.Weekday.MONDAY, "18:30", "19:00", true)),
            Partner.Status.KOOPERIERT,
            null,
            null
        );

        Partner created = controller.create(payload).getBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Bäckerei Sonnenschein");
        assertThat(created.pickupSlots()).hasSize(1);

        Partner fetched = controller.get(created.id()).getBody();
        assertThat(fetched).isNotNull();
        assertThat(fetched.categoryId()).isEqualTo(bakeryId);
        assertThat(fetched.pickupSlots().get(0).weekday()).isEqualTo(Partner.Weekday.MONDAY);

        assertThat(controller.list(false)).hasSize(1);

        Partner updatedPayload = new Partner(
            created.id(), "Bäckerei Sonne", cafeId,
            "Hauptstraße 42", "10115", "Berlin", null,
            payload.contact(), payload.pickupSlots(), Partner.Status.VERHANDLUNGEN_LAUFEN, null, null
        );
        Partner updated = controller.update(created.id(), updatedPayload).getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.name()).isEqualTo("Bäckerei Sonne");
        assertThat(updated.categoryId()).isEqualTo(cafeId);
        assertThat(updated.status()).isEqualTo(Partner.Status.VERHANDLUNGEN_LAUFEN);
    }

    @Test
    void butcherCategoryRoundTrip() {
        Partner created = controller.create(new Partner(
            null, "Metzgerei Müller", butcherId,
            "Wurststraße 1", "10115", "Berlin", null,
            new Partner.Contact("Max Müller", "max@example.de", "+49 176 0000000"),
            List.of(), Partner.Status.KOOPERIERT, null, null
        )).getBody();
        assertThat(created).isNotNull();
        assertThat(created.categoryId()).isEqualTo(butcherId);

        Partner fetched = controller.get(created.id()).getBody();
        assertThat(fetched).isNotNull();
        assertThat(fetched.categoryId()).isEqualTo(butcherId);
    }

    @Test
    void updateAddsAndRemovesSlots() {
        Partner created = controller.create(new Partner(
            null, "Test", bakeryId, "s", "p", "c", null,
            new Partner.Contact("a", "b", "c"),
            List.of(new Partner.PickupSlot(Partner.Weekday.MONDAY, "09:00", "10:00", true)),
            Partner.Status.KOOPERIERT, null, null
        )).getBody();
        assertThat(created).isNotNull();

        Partner withTwo = new Partner(
            created.id(), created.name(), created.categoryId(), created.street(),
            created.postalCode(), created.city(), created.logoUrl(), created.contact(),
            List.of(
                new Partner.PickupSlot(Partner.Weekday.MONDAY, "09:00", "10:00", true),
                new Partner.PickupSlot(Partner.Weekday.FRIDAY, "17:00", "18:00", true)
            ),
            created.status(), null, null
        );
        Partner updated = controller.update(created.id(), withTwo).getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.pickupSlots()).hasSize(2);

        Partner removed = new Partner(
            created.id(), created.name(), created.categoryId(), created.street(),
            created.postalCode(), created.city(), created.logoUrl(), created.contact(),
            List.of(new Partner.PickupSlot(Partner.Weekday.FRIDAY, "17:00", "18:00", true)),
            created.status(), null, null
        );
        Partner reduced = controller.update(created.id(), removed).getBody();
        assertThat(reduced).isNotNull();
        assertThat(reduced.pickupSlots()).hasSize(1);
        assertThat(reduced.pickupSlots().get(0).weekday()).isEqualTo(Partner.Weekday.FRIDAY);
    }

    @Test
    void updateAllowsMultipleSlotsOnSameWeekday() {
        Partner created = controller.create(new Partner(
            null, "Test", bakeryId, "s", "p", "c", null,
            new Partner.Contact("a", "b", "c"),
            List.of(
                new Partner.PickupSlot(Partner.Weekday.FRIDAY, "09:00", "10:00", true),
                new Partner.PickupSlot(Partner.Weekday.FRIDAY, "17:00", "18:00", true)
            ),
            Partner.Status.KOOPERIERT, null, null
        )).getBody();
        assertThat(created).isNotNull();
        assertThat(created.pickupSlots()).hasSize(2);
    }

    @Test
    void updateRejectsRemovingSlotWithFutureScheduledPickup() {
        Partner created = controller.create(new Partner(
            null, "Test", bakeryId, "s", "p", "c", null,
            new Partner.Contact("a", "b", "c"),
            List.of(new Partner.PickupSlot(Partner.Weekday.MONDAY, "09:00", "10:00", true)),
            Partner.Status.KOOPERIERT, null, null
        )).getBody();
        assertThat(created).isNotNull();

        LocalDate nextMonday = LocalDate.now();
        while (nextMonday.getDayOfWeek() != DayOfWeek.MONDAY || !nextMonday.isAfter(LocalDate.now())) {
            nextMonday = nextMonday.plusDays(1);
        }
        pickupService.create(new Pickup(
            null, created.id(), null, null, null, null, null,
            nextMonday, "09:00", "10:00", Pickup.Status.SCHEDULED, 1, List.of(), null, null
        ));

        Partner withoutSlot = new Partner(
            created.id(), created.name(), created.categoryId(), created.street(),
            created.postalCode(), created.city(), created.logoUrl(), created.contact(),
            List.of(), created.status(), null, null
        );
        assertThatThrownBy(() -> controller.update(created.id(), withoutSlot))
            .isInstanceOf(PartnerService.SlotInUseException.class);
    }

    @Test
    void statusChangeAutoCreatesInternalNote() {
        Partner created = controller.create(new Partner(
            null, "Test", bakeryId, "s", "p", "c", null,
            new Partner.Contact("a", "b", "c"),
            List.of(), Partner.Status.KEIN_KONTAKT, null, null
        )).getBody();
        assertThat(created).isNotNull();

        assertThat(noteController.list(created.id())).isEmpty();

        Partner statusChange = new Partner(
            created.id(), created.name(), created.categoryId(), created.street(),
            created.postalCode(), created.city(), created.logoUrl(), created.contact(),
            created.pickupSlots(), Partner.Status.KOOPERIERT, null, null
        );
        controller.update(created.id(), statusChange);

        List<PartnerNote> notes = noteController.list(created.id());
        assertThat(notes).hasSize(1);
        PartnerNote note = notes.get(0);
        assertThat(note.visibility()).isEqualTo(Visibility.INTERNAL);
        assertThat(note.body()).isEqualTo("Status geändert: Kein Kontakt → Kooperiert mit uns");
        assertThat(note.authorUserId()).isEqualTo(adminId);

        Partner nameOnlyChange = new Partner(
            created.id(), "Anderer Name", created.categoryId(), created.street(),
            created.postalCode(), created.city(), created.logoUrl(), created.contact(),
            created.pickupSlots(), Partner.Status.KOOPERIERT, null, null
        );
        controller.update(created.id(), nameOnlyChange);
        assertThat(noteController.list(created.id())).hasSize(1);

        controller.delete(created.id());
        List<PartnerNote> afterDelete = noteController.list(created.id());
        assertThat(afterDelete).hasSize(2);
        assertThat(afterDelete.get(0).body())
            .isEqualTo("Status geändert: Kooperiert mit uns → Existiert nicht mehr");
    }

    @Test
    void updateMissingReturns404() {
        Partner partner = new Partner(999L, "X", cafeId, "s", "p", "c",
            null, new Partner.Contact("a", "b", "c"), List.of(), Partner.Status.KOOPERIERT, null, null);
        assertThat(controller.update(999L, partner).getStatusCode().value()).isEqualTo(404);
    }
}
