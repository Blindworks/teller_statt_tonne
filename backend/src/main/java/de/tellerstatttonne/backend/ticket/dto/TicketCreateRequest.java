package de.tellerstatttonne.backend.ticket.dto;

import de.tellerstatttonne.backend.ticket.TicketCategory;

public record TicketCreateRequest(
    String title,
    String description,
    TicketCategory category
) {}
