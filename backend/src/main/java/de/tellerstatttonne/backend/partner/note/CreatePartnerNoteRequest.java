package de.tellerstatttonne.backend.partner.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePartnerNoteRequest(
    @NotBlank @Size(max = 4000) String body,
    @NotNull Visibility visibility
) {}
