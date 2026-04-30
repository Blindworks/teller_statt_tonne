package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
    ) {}

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {}

    public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        User user
    ) {}
}
