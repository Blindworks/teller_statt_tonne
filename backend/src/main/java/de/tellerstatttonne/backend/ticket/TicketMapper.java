package de.tellerstatttonne.backend.ticket;

import de.tellerstatttonne.backend.ticket.dto.Ticket;
import de.tellerstatttonne.backend.ticket.dto.TicketAttachment;
import de.tellerstatttonne.backend.ticket.dto.TicketComment;
import de.tellerstatttonne.backend.ticket.dto.TicketSummary;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class TicketMapper {

    private TicketMapper() {}

    static Ticket toDto(
        TicketEntity entity,
        List<TicketAttachmentEntity> attachments,
        List<TicketCommentEntity> comments,
        Function<Long, String> userNameLookup
    ) {
        return new Ticket(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getCategory(),
            entity.getStatus(),
            entity.getCreatedById(),
            userNameLookup.apply(entity.getCreatedById()),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            attachments.stream().map(TicketMapper::toAttachmentDto).toList(),
            comments.stream().map(c -> toCommentDto(c, userNameLookup)).toList()
        );
    }

    static TicketSummary toSummary(
        TicketEntity entity,
        int commentCount,
        int attachmentCount,
        Function<Long, String> userNameLookup
    ) {
        return new TicketSummary(
            entity.getId(),
            entity.getTitle(),
            entity.getCategory(),
            entity.getStatus(),
            entity.getCreatedById(),
            userNameLookup.apply(entity.getCreatedById()),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            commentCount,
            attachmentCount
        );
    }

    static TicketAttachment toAttachmentDto(TicketAttachmentEntity entity) {
        return new TicketAttachment(
            entity.getId(),
            entity.getUrl(),
            entity.getOriginalFilename(),
            entity.getUploadedById(),
            entity.getUploadedAt()
        );
    }

    static TicketComment toCommentDto(TicketCommentEntity entity, Function<Long, String> userNameLookup) {
        return new TicketComment(
            entity.getId(),
            entity.getBody(),
            entity.getAuthorId(),
            userNameLookup.apply(entity.getAuthorId()),
            entity.getCreatedAt()
        );
    }

    static Map<Long, Long> emptyMap() {
        return Map.of();
    }
}
