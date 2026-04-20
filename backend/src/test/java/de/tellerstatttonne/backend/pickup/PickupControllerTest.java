package de.tellerstatttonne.backend.pickup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.member.Member;
import de.tellerstatttonne.backend.member.MemberService;
import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PickupControllerTest {

    @Autowired private PickupController controller;
    @Autowired private PickupRepository pickupRepository;
    @Autowired private PartnerService partnerService;
    @Autowired private MemberService memberService;

    private String partnerId;
    private String memberId;

    @BeforeEach
    void setup() {
        pickupRepository.deleteAll();
        Partner partner = partnerService.create(new Partner(
            null, "Bio-Markt Sonne", Partner.Category.SUPERMARKET,
            "Hauptstraße 1", "10115", "Berlin", null,
            new Partner.Contact("Ina", "ina@example.de", "+49 30 111"),
            List.of(), Partner.Status.ACTIVE
        ));
        partnerId = partner.id();

        Member member = memberService.create(new Member(
            null, "Lisa", "Muster", Member.Type.FOODSAVER, null,
            "lisa@example.de", "+49 30 222", "Berlin", null,
            Member.OnlineStatus.ONLINE, Member.Status.ACTIVE, List.of()
        ));
        memberId = member.id();
    }

    @Test
    void createListUpdateDeleteRoundTrip() {
        Pickup payload = new Pickup(
            null, partnerId, null, null,
            LocalDate.of(2026, 4, 21), "19:30", "20:00",
            Pickup.Status.SCHEDULED, 2,
            List.of(new Pickup.Assignment(memberId, null, null)),
            "Bitte Rückseite benutzen"
        );

        Pickup created = controller.create(payload).getBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotBlank();
        assertThat(created.partnerName()).isEqualTo("Bio-Markt Sonne");
        assertThat(created.partnerCategory()).isEqualTo(Partner.Category.SUPERMARKET);
        assertThat(created.assignments()).hasSize(1);
        assertThat(created.assignments().get(0).memberName()).isEqualTo("Lisa Muster");

        List<Pickup> week = controller.list(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));
        assertThat(week).hasSize(1);

        List<Pickup> recent = controller.recent();
        assertThat(recent).hasSize(1);

        Pickup updatedPayload = new Pickup(
            created.id(), partnerId, null, null,
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
            null, partnerId, null, null,
            LocalDate.of(2026, 4, 21), "20:00", "19:00",
            Pickup.Status.SCHEDULED, 1, List.of(), null
        );
        assertThatThrownBy(() -> controller.create(bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCapacityBelowAssignments() {
        Pickup bad = new Pickup(
            null, partnerId, null, null,
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
            "nope", partnerId, null, null,
            LocalDate.of(2026, 4, 21), "10:00", "10:30",
            Pickup.Status.SCHEDULED, 1, List.of(), null
        );
        assertThat(controller.update("nope", payload).getStatusCode().value()).isEqualTo(404);
    }
}
