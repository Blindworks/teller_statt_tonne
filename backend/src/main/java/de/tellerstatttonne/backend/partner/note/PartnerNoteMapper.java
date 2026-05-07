package de.tellerstatttonne.backend.partner.note;

import de.tellerstatttonne.backend.user.UserEntity;

final class PartnerNoteMapper {

    private PartnerNoteMapper() {}

    static PartnerNote toDto(PartnerNoteEntity entity) {
        UserEntity author = entity.getAuthor();
        String displayName = author == null
            ? ""
            : (author.getFirstName() + " " + author.getLastName()).trim();
        return new PartnerNote(
            entity.getId(),
            entity.getPartner().getId(),
            entity.getBody(),
            entity.getVisibility(),
            entity.getCreatedAt(),
            author == null ? null : author.getId(),
            displayName,
            entity.getDeletedAt() != null
        );
    }
}
