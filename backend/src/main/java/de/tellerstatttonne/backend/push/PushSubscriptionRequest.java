package de.tellerstatttonne.backend.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubscriptionRequest(
    @NotBlank @Size(max = 1024) String endpoint,
    @NotBlank @Size(max = 255) String p256dh,
    @NotBlank @Size(max = 255) String auth,
    @Size(max = 512) String userAgent
) {}
