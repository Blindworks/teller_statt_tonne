package de.tellerstatttonne.backend.ticket.dto;

import de.tellerstatttonne.backend.ticket.TicketCategory;
import de.tellerstatttonne.backend.ticket.TicketStatus;
import java.time.Instant;
import java.util.List;

public record Ticket(
    Long id,
    String title,
    String description,
    TicketCategory category,
    TicketStatus status,
    Long createdById,
    String createdByName,
    Instant createdAt,
    Instant updatedAt,
    List<TicketAttachment> attachments,
    List<TicketComment> comments
) {}
