package de.tellerstatttonne.backend.auth.passwordreset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {

    private PasswordResetDtos() {}

    public record ForgotPasswordRequest(
        @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}
}
