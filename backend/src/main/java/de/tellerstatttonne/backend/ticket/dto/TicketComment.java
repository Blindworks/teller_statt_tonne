package de.tellerstatttonne.backend.ticket.dto;

import java.time.Instant;

public record TicketComment(
    Long id,
    String body,
    Long authorId,
    String authorName,
    Instant createdAt
) {}
