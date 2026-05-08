package de.tellerstatttonne.backend.hygiene;

import de.tellerstatttonne.backend.notification.NotificationService;
import de.tellerstatttonne.backend.notification.NotificationType;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleRepository;
import de.tellerstatttonne.backend.storage.DocumentStorageService;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class HygieneCertificateService {

    static final String ROLE_ADMIN = "ADMINISTRATOR";
    static final String ROLE_TEAMLEITER = "TEAMLEITER";
    static final String ROLE_RETTER = "RETTER";
    static final String ROLE_NEW_MEMBER = "NEW_MEMBER";

    private final HygieneCertificateRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DocumentStorageService storage;
    private final NotificationService notificationService;

    public HygieneCertificateService(
        HygieneCertificateRepository repository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        DocumentStorageService storage,
        NotificationService notificationService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storage = storage;
        this.notificationService = notificationService;
    }

    public HygieneCertificateDto submit(Long userId, MultipartFile file, LocalDate issuedDate) {
        if (issuedDate == null) {
            throw new IllegalArgumentException("issuedDate is required");
        }
        if (issuedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("issuedDate must not be in the future");
        }
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));

        Optional<HygieneCertificateEntity> existingOpt = repository.findByUserId(userId);
        String previousFileUrl = existingOpt.map(HygieneCertificateEntity::getFileUrl).orElse(null);

        DocumentStorageService.StoredDocument stored = storage.store(
            DocumentStorageService.CERTIFICATES_SUBDIR,
            userId.toString(),
            file,
            previousFileUrl
        );

        HygieneCertificateEntity entity = existingOpt.orElseGet(HygieneCertificateEntity::new);
        entity.setUser(user);
        entity.setFileUrl(stored.relativePath());
        entity.setMimeType(stored.mimeType());
        entity.setOriginalFilename(stored.originalFilename());
        entity.setFileSizeBytes(stored.sizeBytes());
        entity.setIssuedDate(issuedDate);
        entity.setStatus(HygieneCertificateStatus.PENDING);
        entity.setRejectionReason(null);
        entity.setDecidedBy(null);
        entity.setDecidedAt(null);

        HygieneCertificateEntity saved = repository.save(entity);
        notifyDeciders(saved);
        return HygieneCertificateMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<HygieneCertificateDto> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(HygieneCertificateMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<HygieneCertificateEntity> loadEntityForFile(Long userId) {
        return repository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<HygieneCertificateEntity> loadEntityById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<HygieneCertificateDto> listByStatus(HygieneCertificateStatus status) {
        HygieneCertificateStatus filter = status == null ? HygieneCertificateStatus.PENDING : status;
        return repository.findByStatusOrderByCreatedAtAsc(filter).stream()
            .map(HygieneCertificateMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByStatus(HygieneCertificateStatus.PENDING);
    }

    public HygieneCertificateDto approve(Long certificateId, Long deciderUserId) {
        HygieneCertificateEntity entity = loadPending(certificateId);
        UserEntity decider = requireDecider(deciderUserId);

        UserEntity user = entity.getUser();
        ensureRole(user, ROLE_RETTER);
        removeRole(user, ROLE_NEW_MEMBER);
        userRepository.save(user);

        entity.setStatus(HygieneCertificateStatus.APPROVED);
        entity.setRejectionReason(null);
        entity.setDecidedBy(decider);
        entity.setDecidedAt(Instant.now());
        HygieneCertificateEntity saved = repository.save(entity);

        notificationService.create(
            List.of(user.getId()),
            NotificationType.HYGIENE_CERTIFICATE_APPROVED,
            "Hygienezertifikat genehmigt",
            "Dein Hygienezertifikat wurde genehmigt. Du bist jetzt als Retter freigeschaltet.",
            null,
            null,
            decider.getId()
        );
        return HygieneCertificateMapper.toDto(saved);
    }

    public HygieneCertificateDto reject(Long certificateId, Long deciderUserId, String reason) {
        HygieneCertificateEntity entity = loadPending(certificateId);
        UserEntity decider = requireDecider(deciderUserId);
        String trimmed = reason != null ? reason.trim() : null;
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("reason is required for reject");
        }

        entity.setStatus(HygieneCertificateStatus.REJECTED);
        entity.setRejectionReason(trimmed);
        entity.setDecidedBy(decider);
        entity.setDecidedAt(Instant.now());
        HygieneCertificateEntity saved = repository.save(entity);

        notificationService.create(
            List.of(entity.getUser().getId()),
            NotificationType.HYGIENE_CERTIFICATE_REJECTED,
            "Hygienezertifikat abgelehnt",
            "Dein Hygienezertifikat wurde abgelehnt. Grund: " + trimmed,
            null,
            null,
            decider.getId()
        );
        return HygieneCertificateMapper.toDto(saved);
    }

    public boolean canAccess(Long certificateOwnerUserId, Long requesterUserId) {
        if (certificateOwnerUserId == null || requesterUserId == null) return false;
        if (certificateOwnerUserId.equals(requesterUserId)) return true;
        return userRepository.findById(requesterUserId)
            .map(u -> u.hasRole(ROLE_ADMIN) || u.hasRole(ROLE_TEAMLEITER))
            .orElse(false);
    }

    private HygieneCertificateEntity loadPending(Long id) {
        HygieneCertificateEntity entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("certificate not found: " + id));
        if (entity.getStatus() != HygieneCertificateStatus.PENDING) {
            throw new IllegalStateException("certificate is not PENDING");
        }
        return entity;
    }

    private UserEntity requireDecider(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        if (!user.hasRole(ROLE_ADMIN) && !user.hasRole(ROLE_TEAMLEITER)) {
            throw new AccessDeniedException("only Admin or Teamleiter may decide on certificates");
        }
        return user;
    }

    private void ensureRole(UserEntity user, String roleName) {
        if (user.hasRole(roleName)) return;
        RoleEntity role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Role missing in database: " + roleName));
        user.getRoles().add(role);
    }

    private void removeRole(UserEntity user, String roleName) {
        user.getRoles().removeIf(r -> roleName.equals(r.getName()));
    }

    private void notifyDeciders(HygieneCertificateEntity certificate) {
        List<Long> recipients = userRepository.findAll().stream()
            .filter(u -> u.hasRole(ROLE_ADMIN) || u.hasRole(ROLE_TEAMLEITER))
            .map(UserEntity::getId)
            .toList();
        if (recipients.isEmpty()) return;
        UserEntity user = certificate.getUser();
        String name = displayName(user);
        notificationService.create(
            recipients,
            NotificationType.HYGIENE_CERTIFICATE_SUBMITTED,
            "Neues Hygienezertifikat",
            name + " hat ein Hygienezertifikat zur Prüfung hochgeladen.",
            null,
            null,
            user.getId()
        );
    }

    private static String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String result = (first + " " + last).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}
