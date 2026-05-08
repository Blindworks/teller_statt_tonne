package de.tellerstatttonne.backend.hygiene;

import de.tellerstatttonne.backend.user.UserEntity;

final class HygieneCertificateMapper {

    private HygieneCertificateMapper() {}

    static HygieneCertificateDto toDto(HygieneCertificateEntity entity) {
        UserEntity user = entity.getUser();
        UserEntity decider = entity.getDecidedBy();
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
            entity.getStatus(),
            entity.getRejectionReason(),
            decider != null ? decider.getId() : null,
            decider != null ? displayName(decider) : null,
            entity.getDecidedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private static String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String result = (first + " " + last).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}
