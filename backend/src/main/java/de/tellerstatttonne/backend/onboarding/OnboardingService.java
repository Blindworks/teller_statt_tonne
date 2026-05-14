package de.tellerstatttonne.backend.onboarding;

import de.tellerstatttonne.backend.hygiene.HygieneCertificateRepository;
import de.tellerstatttonne.backend.hygiene.HygieneCertificateStatus;
import de.tellerstatttonne.backend.introduction.IntroductionBookingEntity;
import de.tellerstatttonne.backend.introduction.IntroductionSlotService;
import de.tellerstatttonne.backend.storage.DocumentStorageService;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import de.tellerstatttonne.backend.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class OnboardingService {

    public static final String AGREEMENT_SUBDIR = "agreements";

    private final UserRepository userRepository;
    private final HygieneCertificateRepository hygieneRepository;
    private final IntroductionSlotService introductionSlotService;
    private final DocumentStorageService storageService;
    private final UserService userService;

    public OnboardingService(UserRepository userRepository,
                             HygieneCertificateRepository hygieneRepository,
                             IntroductionSlotService introductionSlotService,
                             DocumentStorageService storageService,
                             UserService userService) {
        this.userRepository = userRepository;
        this.hygieneRepository = hygieneRepository;
        this.introductionSlotService = introductionSlotService;
        this.storageService = storageService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public OnboardingStatusDto getStatus(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        return computeStatus(user);
    }

    public OnboardingStatusDto uploadAgreement(Long userId, MultipartFile file) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        DocumentStorageService.StoredDocument stored = storageService.store(
            AGREEMENT_SUBDIR, "agreement-" + userId, file, user.getAgreementFileUrl());
        user.setAgreementFileUrl(stored.relativePath());
        user.setAgreementUploadedAt(Instant.now());
        userRepository.save(user);
        userService.promoteToActiveIfReady(userId);
        return computeStatus(userRepository.findById(userId).orElse(user));
    }

    public OnboardingStatusDto setTestPickupCompleted(Long userId, boolean completed) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
        user.setTestPickupCompletedAt(completed ? Instant.now() : null);
        userRepository.save(user);
        if (completed) {
            userService.promoteToActiveIfReady(userId);
        }
        return computeStatus(userRepository.findById(userId).orElse(user));
    }

    private OnboardingStatusDto computeStatus(UserEntity user) {
        boolean hygiene = hygieneRepository.findByUserId(user.getId())
            .map(c -> c.getStatus() == HygieneCertificateStatus.APPROVED)
            .orElse(false);
        boolean introduction = user.getIntroductionCompletedAt() != null;
        boolean profile = isProfileComplete(user);
        boolean agreement = user.getAgreementUploadedAt() != null;
        boolean testPickup = user.getTestPickupCompletedAt() != null;
        boolean all = hygiene && introduction && profile && agreement && testPickup;
        boolean activated = user.getStatus() == UserEntity.Status.ACTIVE;

        IntroductionBookingEntity activeBooking =
            introductionSlotService.findActiveBooking(user.getId()).orElse(null);
        return new OnboardingStatusDto(
            hygiene, introduction, profile, agreement, testPickup, all, activated,
            activeBooking != null ? activeBooking.getId() : null,
            activeBooking != null ? activeBooking.getSlotId() : null
        );
    }

    static boolean isProfileComplete(UserEntity user) {
        return notBlank(user.getPhone())
            && notBlank(user.getStreet())
            && notBlank(user.getPostalCode())
            && notBlank(user.getCity());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
