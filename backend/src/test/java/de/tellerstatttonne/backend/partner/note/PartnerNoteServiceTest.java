package de.tellerstatttonne.backend.partner.note;

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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PartnerNoteServiceTest {

    @Autowired private PartnerNoteService service;
    @Autowired private PartnerNoteRepository noteRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Test
    void createPersistsNote() {
        Long partnerId = createPartner().getId();
        Long authorId = createUser("BOTSCHAFTER").getId();

        PartnerNote saved = service.create(
            partnerId,
            new CreatePartnerNoteRequest("Erstes Gespräch verlief gut.", Visibility.INTERNAL),
            authorId
        );

        assertThat(saved.id()).isNotNull();
        assertThat(saved.body()).isEqualTo("Erstes Gespräch verlief gut.");
        assertThat(saved.visibility()).isEqualTo(Visibility.INTERNAL);
        assertThat(saved.createdAt()).isNotNull();
    }

    @Test
    void retterSeesOwnAndShared_butNotForeignInternal() {
        Long partnerId = createPartner().getId();
        Long retterA = createUser("RETTER").getId();
        Long retterB = createUser("RETTER").getId();
        Long botschafter = createUser("BOTSCHAFTER").getId();

        service.create(partnerId, new CreatePartnerNoteRequest("Botschafter intern", Visibility.INTERNAL), botschafter);
        service.create(partnerId, new CreatePartnerNoteRequest("Botschafter shared", Visibility.SHARED), botschafter);
        service.create(partnerId, new CreatePartnerNoteRequest("Retter A eigene", Visibility.SHARED), retterA);
        service.create(partnerId, new CreatePartnerNoteRequest("Retter B eigene", Visibility.SHARED), retterB);

        List<PartnerNote> visibleToA = service.listForUser(partnerId, retterA);
        assertThat(visibleToA).extracting(PartnerNote::body)
            .containsExactlyInAnyOrder("Botschafter shared", "Retter A eigene", "Retter B eigene");
        assertThat(visibleToA).extracting(PartnerNote::body)
            .doesNotContain("Botschafter intern");
    }

    @Test
    void botschafterAndAdminSeeAllUndeleted() {
        Long partnerId = createPartner().getId();
        Long botschafter = createUser("BOTSCHAFTER").getId();
        Long admin = createUser("ADMINISTRATOR").getId();
        Long retter = createUser("RETTER").getId();

        service.create(partnerId, new CreatePartnerNoteRequest("intern", Visibility.INTERNAL), botschafter);
        service.create(partnerId, new CreatePartnerNoteRequest("shared", Visibility.SHARED), retter);

        assertThat(service.listForUser(partnerId, botschafter)).hasSize(2);
        assertThat(service.listForUser(partnerId, admin)).hasSize(2);
    }

    @Test
    void retterAttemptingInternalIsCoercedToShared() {
        Long partnerId = createPartner().getId();
        Long retter = createUser("RETTER").getId();

        PartnerNote saved = service.create(
            partnerId,
            new CreatePartnerNoteRequest("Versuch intern", Visibility.INTERNAL),
            retter
        );

        assertThat(saved.visibility()).isEqualTo(Visibility.SHARED);
    }

    @Test
    void retterCannotDelete() {
        Long partnerId = createPartner().getId();
        Long botschafter = createUser("BOTSCHAFTER").getId();
        Long retter = createUser("RETTER").getId();
        PartnerNote note = service.create(partnerId, new CreatePartnerNoteRequest("x", Visibility.SHARED), botschafter);

        assertThatThrownBy(() -> service.softDelete(note.id(), retter))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void softDeletedNoteHiddenFromList() {
        Long partnerId = createPartner().getId();
        Long botschafter = createUser("BOTSCHAFTER").getId();
        PartnerNote note = service.create(partnerId, new CreatePartnerNoteRequest("zu löschen", Visibility.INTERNAL), botschafter);

        boolean ok = service.softDelete(note.id(), botschafter);
        assertThat(ok).isTrue();

        assertThat(service.listForUser(partnerId, botschafter)).isEmpty();
        assertThat(noteRepository.findById(note.id())).isPresent();
        assertThat(noteRepository.findById(note.id()).orElseThrow().getDeletedAt()).isNotNull();
    }

    private PartnerEntity createPartner() {
        PartnerEntity entity = new PartnerEntity();
        entity.setName("Testbetrieb " + System.nanoTime());
        entity.setCategory(Partner.Category.BAKERY);
        entity.setStatus(Partner.Status.KEIN_KONTAKT);
        return partnerRepository.save(entity);
    }

    private UserEntity createUser(String roleName) {
        RoleEntity role = roleRepository.findByName(roleName).orElseThrow();
        UserEntity user = new UserEntity();
        user.setEmail("note-test-" + System.nanoTime() + "@example.de");
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
