package de.tellerstatttonne.backend.partner.application;

import de.tellerstatttonne.backend.user.UserEntity;

final class PartnerApplicationMapper {

    private PartnerApplicationMapper() {}

    static PartnerApplicationDto toDto(PartnerApplicationEntity e) {
        UserEntity user = e.getUser();
        UserEntity decidedBy = e.getDecidedBy();
        return new PartnerApplicationDto(
            e.getId(),
            e.getPartner().getId(),
            e.getPartner().getName(),
            e.getPartner().getStreet(),
            e.getPartner().getPostalCode(),
            e.getPartner().getCity(),
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhotoUrl(),
            e.getStatus(),
            e.getMessage(),
            e.getDecisionReason(),
            decidedBy != null ? decidedBy.getId() : null,
            decidedBy != null ? displayName(decidedBy) : null,
            e.getCreatedAt(),
            e.getUpdatedAt(),
            e.getDecidedAt()
        );
    }

    private static String displayName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String result = (first + " " + last).trim();
        return result.isEmpty() ? user.getEmail() : result;
    }
}
