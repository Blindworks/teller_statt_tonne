package de.tellerstatttonne.backend.partner.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePartnerApplicationRequest(
    @NotNull Long partnerId,
    @Size(max = 1000) String message
) {}
