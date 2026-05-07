package de.tellerstatttonne.backend.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.pickup.Pickup;
import de.tellerstatttonne.backend.pickup.PickupRepository;
import de.tellerstatttonne.backend.pickup.PickupService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @BeforeEach
    void cleanSlate() {
        pickupRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void createFetchUpdateRoundTrip() {
        Partner payload = new Partner(
            null,
            "Bäckerei Sonnenschein",
            Partner.Category.BAKERY,
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
        assertThat(fetched.category()).isEqualTo(Partner.Category.BAKERY);
        assertThat(fetched.pickupSlots().get(0).weekday()).isEqualTo(Partner.Weekday.MONDAY);

        assertThat(controller.list(false)).hasSize(1);

        Partner updatedPayload = new Partner(
            created.id(), "Bäckerei Sonne", Partner.Category.CAFE,
            "Hauptstraße 42", "10115", "Berlin", null,
            payload.contact(), payload.pickupSlots(), Partner.Status.VERHANDLUNGEN_LAUFEN, null, null
        );
        Partner updated = controller.update(created.id(), updatedPayload).getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.name()).isEqualTo("Bäckerei Sonne");
        assertThat(updated.category()).isEqualTo(Partner.Category.CAFE);
        assertThat(updated.status()).isEqualTo(Partner.Status.VERHANDLUNGEN_LAUFEN);
    }

    @Test
    void updateAddsAndRemovesSlots() {
        Partner created = controller.create(new Partner(
            null, "Test", Partner.Category.BAKERY, "s", "p", "c", null,
            new Partner.Contact("a", "b", "c"),
            List.of(new Partner.PickupSlot(Partner.Weekday.MONDAY, "09:00", "10:00", true)),
            Partner.Status.KOOPERIERT, null, null
        )).getBody();
        assertThat(created).isNotNull();

        Partner withTwo = new Partner(
            created.id(), created.name(), created.category(), created.street(),
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
            created.id(), created.name(), created.category(), created.street(),
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
            null, "Test", Partner.Category.BAKERY, "s", "p", "c", null,
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
            null, "Test", Partner.Category.BAKERY, "s", "p", "c", null,
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
            nextMonday, "09:00", "10:00", Pickup.Status.SCHEDULED, 1, List.of(), null
        ));

        Partner withoutSlot = new Partner(
            created.id(), created.name(), created.category(), created.street(),
            created.postalCode(), created.city(), created.logoUrl(), created.contact(),
            List.of(), created.status(), null, null
        );
        assertThatThrownBy(() -> controller.update(created.id(), withoutSlot))
            .isInstanceOf(PartnerService.SlotInUseException.class);
    }

    @Test
    void updateMissingReturns404() {
        Partner partner = new Partner(999L, "X", Partner.Category.CAFE, "s", "p", "c",
            null, new Partner.Contact("a", "b", "c"), List.of(), Partner.Status.KOOPERIERT, null, null);
        assertThat(controller.update(999L, partner).getStatusCode().value()).isEqualTo(404);
    }
}
