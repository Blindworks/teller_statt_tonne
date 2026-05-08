package de.tellerstatttonne.backend.hygiene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.storage.DocumentStorageService;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HygieneCertificateServiceTest {

    @Autowired private HygieneCertificateService service;
    @Autowired private HygieneCertificateRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DocumentStorageService storage;

    @Test
    void submitCreatesPendingEntity() {
        Long userId = createUser("NEW_MEMBER").getId();

        HygieneCertificateDto dto = service.submit(userId, samplePdf(), LocalDate.now().minusDays(30));

        assertThat(dto.id()).isNotNull();
        assertThat(dto.status()).isEqualTo(HygieneCertificateStatus.PENDING);
        assertThat(dto.mimeType()).isEqualTo("application/pdf");
    }

    @Test
    void approveAddsRetterRoleAndRemovesNewMember() {
        UserEntity user = createUser("NEW_MEMBER");
        Long deciderId = createUser("TEAMLEITER").getId();
        HygieneCertificateDto submitted = service.submit(user.getId(), samplePdf(), LocalDate.now());

        HygieneCertificateDto approved = service.approve(submitted.id(), deciderId);

        assertThat(approved.status()).isEqualTo(HygieneCertificateStatus.APPROVED);
        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.hasRole("RETTER")).isTrue();
        assertThat(reloaded.hasRole("NEW_MEMBER")).isFalse();
    }

    @Test
    void rejectStoresReason() {
        Long userId = createUser("NEW_MEMBER").getId();
        Long deciderId = createUser("TEAMLEITER").getId();
        HygieneCertificateDto submitted = service.submit(userId, samplePdf(), LocalDate.now());

        HygieneCertificateDto rejected = service.reject(submitted.id(), deciderId, "unleserlich");

        assertThat(rejected.status()).isEqualTo(HygieneCertificateStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("unleserlich");
    }

    @Test
    void rejectRequiresReason() {
        Long userId = createUser("NEW_MEMBER").getId();
        Long deciderId = createUser("TEAMLEITER").getId();
        HygieneCertificateDto submitted = service.submit(userId, samplePdf(), LocalDate.now());

        assertThatThrownBy(() -> service.reject(submitted.id(), deciderId, " "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retterCannotApproveOrReject() {
        Long userId = createUser("NEW_MEMBER").getId();
        Long retterId = createUser("RETTER").getId();
        HygieneCertificateDto submitted = service.submit(userId, samplePdf(), LocalDate.now());

        assertThatThrownBy(() -> service.approve(submitted.id(), retterId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.reject(submitted.id(), retterId, "nope"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reUploadDeletesOldFileAndResetsToPending() throws Exception {
        Long userId = createUser("NEW_MEMBER").getId();
        Long deciderId = createUser("TEAMLEITER").getId();
        HygieneCertificateDto first = service.submit(userId, samplePdf(), LocalDate.now());
        service.reject(first.id(), deciderId, "neu hochladen");

        HygieneCertificateEntity afterReject = repository.findById(first.id()).orElseThrow();
        Path firstPath = storage.resolve(afterReject.getFileUrl());
        assertThat(Files.exists(firstPath)).isTrue();

        HygieneCertificateDto resubmitted = service.submit(userId, samplePdf(), LocalDate.now());

        assertThat(resubmitted.status()).isEqualTo(HygieneCertificateStatus.PENDING);
        assertThat(resubmitted.rejectionReason()).isNull();
        assertThat(Files.exists(firstPath)).isFalse();
        // single row per user enforced by unique constraint
        assertThat(repository.findByUserId(userId)).isPresent();
    }

    @Test
    void futureIssuedDateRejected() {
        Long userId = createUser("NEW_MEMBER").getId();
        assertThatThrownBy(() -> service.submit(userId, samplePdf(), LocalDate.now().plusDays(1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsupportedMimeTypeRejected() {
        Long userId = createUser("NEW_MEMBER").getId();
        MockMultipartFile bad = new MockMultipartFile("file", "doc.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.submit(userId, bad, LocalDate.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canAccessMatrix() {
        Long owner = createUser("NEW_MEMBER").getId();
        Long teamleiter = createUser("TEAMLEITER").getId();
        Long stranger = createUser("RETTER").getId();

        assertThat(service.canAccess(owner, owner)).isTrue();
        assertThat(service.canAccess(owner, teamleiter)).isTrue();
        assertThat(service.canAccess(owner, stranger)).isFalse();
    }

    private MockMultipartFile samplePdf() {
        // minimal PDF header so consumers see proper content
        byte[] body = "%PDF-1.4\n%âãÏÓ\n1 0 obj<<>>endobj\n%%EOF".getBytes();
        return new MockMultipartFile("file", "zertifikat.pdf", "application/pdf", body);
    }

    private UserEntity createUser(String roleName) {
        RoleEntity role = roleRepository.findByName(roleName).orElseThrow();
        UserEntity user = new UserEntity();
        user.setEmail("hyg-test-" + System.nanoTime() + "@example.de");
        user.setPasswordHash("dummy");
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
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
