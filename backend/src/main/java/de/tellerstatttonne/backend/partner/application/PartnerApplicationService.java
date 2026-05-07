package de.tellerstatttonne.backend.partner.application;

import de.tellerstatttonne.backend.notification.NotificationService;
import de.tellerstatttonne.backend.notification.NotificationType;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import de.tellerstatttonne.backend.partner.PartnerMemberService;
import de.tellerstatttonne.backend.partner.PartnerRepository;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartnerApplicationService {

    private static final String ROLE_ADMIN = "ADMINISTRATOR";
    private static final String ROLE_TEAMLEITER = "TEAMLEITER";

    private final PartnerApplicationRepository repository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;
    private final PartnerMemberService memberService;
    private final NotificationService notificationService;

    public PartnerApplicationService(
        PartnerApplicationRepository repository,
        PartnerRepository partnerRepository,
        UserRepository userRepository,
        PartnerMemberService memberService,
        NotificationService notificationService
    ) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.memberService = memberService;
        this.notificationService = notificationService;
    }

    public PartnerApplicationDto apply(Long partnerId, Long userId, String message) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new EntityNotFoundException("partner not found: " + partnerId));
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));

        if (partner.getMembers().stream().anyMatch(m -> userId.equals(m.getId()))) {
            throw new IllegalStateException("user is already a member of this partner");
        }
        if (repository.existsByPartnerIdAndUserIdAndStatus(partnerId, userId, ApplicationStatus.PENDING)) {
            throw new IllegalStateException("a pending application already exists");
        }

        PartnerApplicationEntity entity = new PartnerApplicationEntity();
        entity.setPartner(partner);
        entity.setUser(user);
        entity.setStatus(ApplicationStatus.PENDING);
        entity.setMessage(message != null ? message.trim() : null);
        PartnerApplicationEntity saved = repository.save(entity);

        notifyDeciders(saved);
        return PartnerApplicationMapper.toDto(saved);
    }

    public PartnerApplicationDto withdraw(Long applicationId, Long userId) {
        PartnerApplicationEntity entity = repository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("application not found: " + applicationId));
        if (!entity.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("only owner can withdraw");
        }
        if (entity.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("only PENDING applications can be withdrawn");
        }
        entity.setStatus(ApplicationStatus.WITHDRAWN);
        entity.setDecidedAt(Instant.now());
        return PartnerApplicationMapper.toDto(repository.save(entity));
    }

    public PartnerApplicationDto approve(Long applicationId, Long deciderUserId) {
        PartnerApplicationEntity entity = loadPending(applicationId);
        UserEntity decider = requireDecider(deciderUserId);

        memberService.assign(entity.getPartner().getId(), entity.getUser().getId());

        entity.setStatus(ApplicationStatus.APPROVED);
        entity.setDecidedBy(decider);
        entity.setDecidedAt(Instant.now());
        PartnerApplicationEntity saved = repository.save(entity);

        notifyApplicant(saved, NotificationType.PARTNER_APPLICATION_APPROVED,
            "Bewerbung angenommen",
            "Deine Bewerbung bei " + saved.getPartner().getName() + " wurde angenommen.");
        return PartnerApplicationMapper.toDto(saved);
    }

    public PartnerApplicationDto reject(Long applicationId, Long deciderUserId, String reason) {
        PartnerApplicationEntity entity = loadPending(applicationId);
        UserEntity decider = requireDecider(deciderUserId);

        entity.setStatus(ApplicationStatus.REJECTED);
        entity.setDecisionReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        entity.setDecidedBy(decider);
        entity.setDecidedAt(Instant.now());
        PartnerApplicationEntity saved = repository.save(entity);

        String body = "Deine Bewerbung bei " + saved.getPartner().getName() + " wurde abgelehnt.";
        if (saved.getDecisionReason() != null) {
            body = body + " Grund: " + saved.getDecisionReason();
        }
        notifyApplicant(saved, NotificationType.PARTNER_APPLICATION_REJECTED,
            "Bewerbung abgelehnt", body);
        return PartnerApplicationMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PartnerApplicationDto> listForPartner(Long partnerId, ApplicationStatus statusFilter) {
        List<PartnerApplicationEntity> rows = statusFilter != null
            ? repository.findByPartnerIdAndStatusOrderByCreatedAtDesc(partnerId, statusFilter)
            : repository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        return rows.stream().map(PartnerApplicationMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerApplicationDto> listForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(PartnerApplicationMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByStatus(ApplicationStatus.PENDING);
    }

    private PartnerApplicationEntity loadPending(Long applicationId) {
        PartnerApplicationEntity entity = repository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("application not found: " + applicationId));
        if (entity.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("application is not PENDING");
        }
        return entity;
    }

    private UserEntity requireDecider(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        if (!user.hasRole(ROLE_ADMIN) && !user.hasRole(ROLE_TEAMLEITER)) {
            throw new AccessDeniedException("only Admin or Teamleiter may decide on applications");
        }
        return user;
    }

    private void notifyDeciders(PartnerApplicationEntity application) {
        List<Long> recipients = userRepository.findAll().stream()
            .filter(u -> u.hasRole(ROLE_ADMIN) || u.hasRole(ROLE_TEAMLEITER))
            .map(UserEntity::getId)
            .toList();
        if (recipients.isEmpty()) return;
        String applicantName = displayName(application.getUser());
        String title = "Neue Bewerbung";
        String body = applicantName + " hat sich für den Betrieb "
            + application.getPartner().getName() + " beworben.";
        notificationService.create(
            recipients,
            NotificationType.PARTNER_APPLICATION_RECEIVED,
            title,
            body,
            null,
            application.getPartner().getId(),
            application.getUser().getId()
        );
    }

    private void notifyApplicant(PartnerApplicationEntity application, NotificationType type,
                                 String title, String body) {
        notificationService.create(
            List.of(application.getUser().getId()),
            type,
            title,
            body,
            null,
            application.getPartner().getId(),
            application.getDecidedBy() != null ? application.getDecidedBy().getId() : null
        );
    }

    private static String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String result = (first + " " + last).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}
