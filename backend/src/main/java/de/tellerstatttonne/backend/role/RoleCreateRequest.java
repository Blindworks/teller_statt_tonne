package de.tellerstatttonne.backend.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
        message = "name must be UPPER_SNAKE_CASE (A-Z, 0-9, underscore)")
    String name,

    @NotBlank @Size(max = 128) String label,

    String description,

    Integer sortOrder,

    Boolean enabled
) {}
