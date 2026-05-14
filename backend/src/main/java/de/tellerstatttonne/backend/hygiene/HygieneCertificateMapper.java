package de.tellerstatttonne.backend.hygiene;

import de.tellerstatttonne.backend.user.UserEntity;
import java.time.LocalDate;

final class HygieneCertificateMapper {

    private HygieneCertificateMapper() {}

    static HygieneCertificateDto toDto(HygieneCertificateEntity entity, int warningDaysBefore) {
        UserEntity user = entity.getUser();
        UserEntity decider = entity.getDecidedBy();
        LocalDate today = LocalDate.now();
        LocalDate expiry = entity.getExpiryDate();
        Integer daysUntilExpiry = expiry != null
            ? (int) java.time.temporal.ChronoUnit.DAYS.between(today, expiry)
            : null;
        String validityStatus = computeValidityStatus(entity, today, warningDaysBefore);
        return new HygieneCertificateDto(
            entity.getId(),
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhotoUrl(),
            entity.getMimeType(),
            entity.getOriginalFilename(),
            entity.getFileSizeBytes(),
            entity.getIssuedDate(),
            expiry,
            validityStatus,
            daysUntilExpiry,
            entity.getStatus(),
            entity.getRejectionReason(),
            decider != null ? decider.getId() : null,
            decider != null ? displayName(decider) : null,
            entity.getDecidedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private static String computeValidityStatus(HygieneCertificateEntity entity, LocalDate today, int warningDaysBefore) {
        if (entity.getStatus() != HygieneCertificateStatus.APPROVED) {
            return null;
        }
        LocalDate expiry = entity.getExpiryDate();
        if (expiry == null) return null;
        if (expiry.isBefore(today)) return "expired";
        if (!expiry.isAfter(today.plusDays(warningDaysBefore))) return "expiring-soon";
        return "valid";
    }

    private static String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String result = (first + " " + last).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}
