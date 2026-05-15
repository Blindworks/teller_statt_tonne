package de.tellerstatttonne.backend.ticket.dto;

import java.time.Instant;

public record TicketAttachment(
    Long id,
    String url,
    String originalFilename,
    Long uploadedById,
    Instant uploadedAt
) {}
