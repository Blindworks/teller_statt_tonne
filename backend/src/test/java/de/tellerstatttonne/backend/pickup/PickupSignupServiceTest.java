package de.tellerstatttonne.backend.pickup;

import static org.assertj.core.api.Assertions.assertThat;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.partner.PartnerService;
import de.tellerstatttonne.backend.user.Role;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PickupSignupServiceTest {

    @Autowired private PickupSignupService signupService;
    @Autowired private PickupRepository pickupRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private PartnerService partnerService;
    @Autowired private UserRepository userRepository;

    private Long partnerId;
    private Long memberId;
    private Long outsiderId;
    private Long pickupId;

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

        memberId = createUser("member").getId();
        outsiderId = createUser("outsider").getId();

        PartnerEntity partnerEntity = partnerRepository.findById(partnerId).orElseThrow();
        partnerEntity.getMembers().add(userRepository.findById(memberId).orElseThrow());
        partnerRepository.save(partnerEntity);

        PickupEntity pickup = new PickupEntity();
        pickup.setPartner(partnerEntity);
        pickup.setDate(LocalDate.of(2026, 5, 4));
        pickup.setStartTime("18:00");
        pickup.setEndTime("19:00");
        pickup.setCapacity(2);
        pickupId = pickupRepository.save(pickup).getId();
    }

    @Test
    void signupSucceedsForMember() {
        PickupSignupService.Result result = signupService.signup(pickupId, memberId);

        assertThat(result).isEqualTo(PickupSignupService.Result.OK);
        PickupEntity pickup = pickupRepository.findById(pickupId).orElseThrow();
        assertThat(pickup.getAssignments()).hasSize(1);
        assertThat(pickup.getAssignments().get(0).getUserId()).isEqualTo(memberId);
    }

    @Test
    void signupIsIdempotent() {
        signupService.signup(pickupId, memberId);
        PickupSignupService.Result second = signupService.signup(pickupId, memberId);

        assertThat(second).isEqualTo(PickupSignupService.Result.OK);
        assertThat(pickupRepository.findById(pickupId).orElseThrow().getAssignments()).hasSize(1);
    }

    @Test
    void signupForbiddenForNonMember() {
        PickupSignupService.Result result = signupService.signup(pickupId, outsiderId);

        assertThat(result).isEqualTo(PickupSignupService.Result.NOT_MEMBER);
        assertThat(pickupRepository.findById(pickupId).orElseThrow().getAssignments()).isEmpty();
    }

    @Test
    void signupConflictsWhenCapacityFull() {
        Long secondMemberId = createUser("member2").getId();
        PartnerEntity partnerEntity = partnerRepository.findById(partnerId).orElseThrow();
        partnerEntity.getMembers().add(userRepository.findById(secondMemberId).orElseThrow());
        partnerRepository.save(partnerEntity);

        Long thirdMemberId = createUser("member3").getId();
        partnerEntity = partnerRepository.findById(partnerId).orElseThrow();
        partnerEntity.getMembers().add(userRepository.findById(thirdMemberId).orElseThrow());
        partnerRepository.save(partnerEntity);

        signupService.signup(pickupId, memberId);
        signupService.signup(pickupId, secondMemberId);
        PickupSignupService.Result third = signupService.signup(pickupId, thirdMemberId);

        assertThat(third).isEqualTo(PickupSignupService.Result.CAPACITY_FULL);
    }

    @Test
    void signupReturnsNotFoundForUnknownPickup() {
        assertThat(signupService.signup(999_999L, memberId))
            .isEqualTo(PickupSignupService.Result.PICKUP_NOT_FOUND);
    }

    @Test
    void unassignRemovesEntry() {
        signupService.signup(pickupId, memberId);

        PickupSignupService.Result result = signupService.unassign(pickupId, memberId);

        assertThat(result).isEqualTo(PickupSignupService.Result.OK);
        assertThat(pickupRepository.findById(pickupId).orElseThrow().getAssignments()).isEmpty();
    }

    @Test
    void unassignReturnsNotAssignedWhenMissing() {
        PickupSignupService.Result result = signupService.unassign(pickupId, memberId);
        assertThat(result).isEqualTo(PickupSignupService.Result.NOT_ASSIGNED);
    }

    @Test
    void signupRejectedForPastPickup() {
        PickupEntity pickup = pickupRepository.findById(pickupId).orElseThrow();
        pickup.setDate(LocalDate.now().minusDays(1));
        pickupRepository.save(pickup);

        PickupSignupService.Result result = signupService.signup(pickupId, memberId);

        assertThat(result).isEqualTo(PickupSignupService.Result.PICKUP_PAST);
        assertThat(pickupRepository.findById(pickupId).orElseThrow().getAssignments()).isEmpty();
    }

    @Test
    void unassignRejectedForPastPickup() {
        signupService.signup(pickupId, memberId);
        PickupEntity pickup = pickupRepository.findById(pickupId).orElseThrow();
        pickup.setDate(LocalDate.now().minusDays(1));
        pickupRepository.save(pickup);

        PickupSignupService.Result result = signupService.unassign(pickupId, memberId);

        assertThat(result).isEqualTo(PickupSignupService.Result.PICKUP_PAST);
        assertThat(pickupRepository.findById(pickupId).orElseThrow().getAssignments()).hasSize(1);
    }

    private UserEntity createUser(String prefix) {
        UserEntity user = new UserEntity();
        user.setEmail(prefix + "-" + System.nanoTime() + "@example.de");
        user.setPasswordHash("dummy");
        user.setRole(Role.RETTER);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setOnlineStatus(UserEntity.OnlineStatus.OFFLINE);
        user.setStatus(UserEntity.Status.ACTIVE);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }
}
