package de.tellerstatttonne.backend.ticket.dto;

import de.tellerstatttonne.backend.ticket.TicketCategory;
import de.tellerstatttonne.backend.ticket.TicketStatus;
import java.time.Instant;

public record TicketSummary(
    Long id,
    String title,
    TicketCategory category,
    TicketStatus status,
    Long createdById,
    String createdByName,
    Instant createdAt,
    Instant updatedAt,
    int commentCount,
    int attachmentCount
) {}
