package de.tellerstatttonne.backend.partner;

import static org.assertj.core.api.Assertions.assertThat;

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

    @BeforeEach
    void cleanSlate() {
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
            Partner.Status.ACTIVE,
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

        assertThat(controller.list()).hasSize(1);

        Partner updatedPayload = new Partner(
            created.id(), "Bäckerei Sonne", Partner.Category.CAFE,
            "Hauptstraße 42", "10115", "Berlin", null,
            payload.contact(), payload.pickupSlots(), Partner.Status.INACTIVE, null, null
        );
        Partner updated = controller.update(created.id(), updatedPayload).getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.name()).isEqualTo("Bäckerei Sonne");
        assertThat(updated.category()).isEqualTo(Partner.Category.CAFE);
        assertThat(updated.status()).isEqualTo(Partner.Status.INACTIVE);
    }

    @Test
    void updateMissingReturns404() {
        Partner partner = new Partner(999L, "X", Partner.Category.CAFE, "s", "p", "c",
            null, new Partner.Contact("a", "b", "c"), List.of(), Partner.Status.ACTIVE, null, null);
        assertThat(controller.update(999L, partner).getStatusCode().value()).isEqualTo(404);
    }
}
