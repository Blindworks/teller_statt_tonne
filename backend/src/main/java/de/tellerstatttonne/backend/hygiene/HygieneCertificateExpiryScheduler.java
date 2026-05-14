package de.tellerstatttonne.backend.hygiene;

import de.tellerstatttonne.backend.mail.MailService;
import de.tellerstatttonne.backend.notification.NotificationService;
import de.tellerstatttonne.backend.notification.NotificationType;
import de.tellerstatttonne.backend.pickup.PickupSignupService;
import de.tellerstatttonne.backend.settings.SettingsKeys;
import de.tellerstatttonne.backend.settings.SystemSettingService;
import de.tellerstatttonne.backend.user.UserEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HygieneCertificateExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HygieneCertificateExpiryScheduler.class);

    private final HygieneCertificateRepository repository;
    private final SystemSettingService settings;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final PickupSignupService pickupSignupService;

    public HygieneCertificateExpiryScheduler(HygieneCertificateRepository repository,
                                             SystemSettingService settings,
                                             NotificationService notificationService,
                                             MailService mailService,
                                             PickupSignupService pickupSignupService) {
        this.repository = repository;
        this.settings = settings;
        this.notificationService = notificationService;
        this.mailService = mailService;
        this.pickupSignupService = pickupSignupService;
    }

    @Scheduled(cron = "${app.hygiene.expiry-cron:0 0 3 * * *}")
    @Transactional
    public void runDaily() {
        try {
            sendWarningNotifications();
            handleExpired();
        } catch (RuntimeException e) {
            log.error("Hygiene expiry scheduler failed: {}", e.getMessage(), e);
        }
    }

    void sendWarningNotifications() {
        int warningDays = settings.getInt(
            SettingsKeys.HYGIENE_WARNING_DAYS_BEFORE, SettingsKeys.DEFAULT_HYGIENE_WARNING_DAYS_BEFORE);
        LocalDate target = LocalDate.now().plusDays(warningDays);
        List<HygieneCertificateEntity> candidates =
            repository.findByStatusAndExpiryDateAndWarningSentAtIsNull(HygieneCertificateStatus.APPROVED, target);
        for (HygieneCertificateEntity cert : candidates) {
            UserEntity user = cert.getUser();
            String name = displayName(user);
            String title = "Hygienezertifikat läuft bald ab";
            String body = "Hallo " + name + ", dein Hygienezertifikat läuft am "
                + cert.getExpiryDate() + " ab. Bitte lade ein neues hoch, damit du weiterhin retten kannst.";
            notificationService.create(
                List.of(user.getId()),
                NotificationType.HYGIENE_CERTIFICATE_EXPIRING_SOON,
                title, body, null, null, null);
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                mailService.sendPlainText(user.getEmail(), title, body);
            }
            cert.setWarningSentAt(Instant.now());
            repository.save(cert);
        }
    }

    void handleExpired() {
        LocalDate today = LocalDate.now();
        List<HygieneCertificateEntity> expired =
            repository.findByStatusAndExpiryDateBeforeAndExpiredNoticeSentAtIsNull(
                HygieneCertificateStatus.APPROVED, today);
        for (HygieneCertificateEntity cert : expired) {
            UserEntity user = cert.getUser();
            String name = displayName(user);
            String title = "Hygienezertifikat abgelaufen";
            String body = "Hallo " + name + ", dein Hygienezertifikat ist seit "
                + cert.getExpiryDate() + " abgelaufen. Du wurdest aus zukünftigen Pickups ausgetragen. "
                + "Bitte lade ein neues Zertifikat hoch, um wieder zu retten.";
            List<Long> removedFrom = pickupSignupService.removeFutureSignupsForUser(user.getId());
            log.info("User {} aus {} zukuenftigen Pickups ausgetragen (Zertifikat abgelaufen)",
                user.getId(), removedFrom.size());
            notificationService.create(
                List.of(user.getId()),
                NotificationType.HYGIENE_CERTIFICATE_EXPIRED,
                title, body, null, null, null);
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                mailService.sendPlainText(user.getEmail(), title, body);
            }
            cert.setExpiredNoticeSentAt(Instant.now());
            repository.save(cert);
        }
    }

    private static String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String result = (first + " " + last).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}
