package de.tellerstatttonne.backend.partner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PartnerApplicationServiceTest {

    @Autowired private PartnerApplicationService service;
    @Autowired private PartnerApplicationRepository repository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private de.tellerstatttonne.backend.partnercategory.PartnerCategoryRepository partnerCategoryRepository;

    @Test
    void applyCreatesPendingApplication() {
        Long partnerId = createPartner().getId();
        Long retter = createUser("RETTER").getId();

        PartnerApplicationDto dto = service.apply(partnerId, retter, "Bin motiviert");

        assertThat(dto.id()).isNotNull();
        assertThat(dto.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(dto.message()).isEqualTo("Bin motiviert");
    }

    @Test
    void duplicatePendingApplicationIsRejected() {
        Long partnerId = createPartner().getId();
        Long retter = createUser("RETTER").getId();
        service.apply(partnerId, retter, null);

        assertThatThrownBy(() -> service.apply(partnerId, retter, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approveAddsUserToPartnerMembers() {
        PartnerEntity partner = createPartner();
        Long retter = createUser("RETTER").getId();
        Long teamleiter = createUser("TEAMLEITER").getId();

        PartnerApplicationDto applied = service.apply(partner.getId(), retter, null);
        PartnerApplicationDto approved = service.approve(applied.id(), teamleiter);

        assertThat(approved.status()).isEqualTo(ApplicationStatus.APPROVED);
        PartnerEntity reloaded = partnerRepository.findById(partner.getId()).orElseThrow();
        assertThat(reloaded.getMembers()).extracting(UserEntity::getId).contains(retter);
    }

    @Test
    void rejectStoresReason() {
        Long partnerId = createPartner().getId();
        Long retter = createUser("RETTER").getId();
        Long teamleiter = createUser("TEAMLEITER").getId();

        PartnerApplicationDto applied = service.apply(partnerId, retter, null);
        PartnerApplicationDto rejected = service.reject(applied.id(), teamleiter, "kein Bedarf");

        assertThat(rejected.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(rejected.decisionReason()).isEqualTo("kein Bedarf");
    }

    @Test
    void retterCannotApprove() {
        Long partnerId = createPartner().getId();
        Long retterA = createUser("RETTER").getId();
        Long retterB = createUser("RETTER").getId();
        PartnerApplicationDto applied = service.apply(partnerId, retterA, null);

        assertThatThrownBy(() -> service.approve(applied.id(), retterB))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void withdrawByOwnerWorks() {
        Long partnerId = createPartner().getId();
        Long retter = createUser("RETTER").getId();
        PartnerApplicationDto applied = service.apply(partnerId, retter, null);

        PartnerApplicationDto withdrawn = service.withdraw(applied.id(), retter);

        assertThat(withdrawn.status()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void withdrawByOtherUserDenied() {
        Long partnerId = createPartner().getId();
        Long retterA = createUser("RETTER").getId();
        Long retterB = createUser("RETTER").getId();
        PartnerApplicationDto applied = service.apply(partnerId, retterA, null);

        assertThatThrownBy(() -> service.withdraw(applied.id(), retterB))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotApplyWhenAlreadyMember() {
        PartnerEntity partner = createPartner();
        UserEntity user = createUser("RETTER");
        partner.getMembers().add(user);
        partnerRepository.save(partner);

        assertThatThrownBy(() -> service.apply(partner.getId(), user.getId(), null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void afterRejectNewApplicationCanBeFiled() {
        Long partnerId = createPartner().getId();
        Long retter = createUser("RETTER").getId();
        Long teamleiter = createUser("TEAMLEITER").getId();
        PartnerApplicationDto first = service.apply(partnerId, retter, null);
        service.reject(first.id(), teamleiter, "spaeter");

        PartnerApplicationDto second = service.apply(partnerId, retter, "neuer Versuch");
        assertThat(second.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(repository.findByUserIdOrderByCreatedAtDesc(retter)).hasSize(2);
    }

    private PartnerEntity createPartner() {
        PartnerEntity entity = new PartnerEntity();
        entity.setName("Testbetrieb " + System.nanoTime());
        entity.setCategoryId(partnerCategoryRepository.findByCodeIgnoreCase("BAKERY").orElseThrow().getId());
        entity.setStatus(Partner.Status.KOOPERIERT);
        return partnerRepository.save(entity);
    }

    private UserEntity createUser(String roleName) {
        RoleEntity role = roleRepository.findByName(roleName).orElseThrow();
        UserEntity user = new UserEntity();
        user.setEmail("appl-test-" + System.nanoTime() + "@example.de");
        user.setPasswordHash("dummy");
        user.setRoles(Set.of(role));
        user.setFirstName("Vor");
        user.setLastName("Nach");
        user.setOnlineStatus(UserEntity.OnlineStatus.OFFLINE);
        user.setStatus(UserEntity.Status.ACTIVE);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }
}
