package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.auth.AuthDtos.AuthResponse;
import de.tellerstatttonne.backend.auth.AuthDtos.ChangePasswordRequest;
import de.tellerstatttonne.backend.auth.AuthDtos.LoginRequest;
import de.tellerstatttonne.backend.auth.AuthDtos.RefreshRequest;
import de.tellerstatttonne.backend.auth.passwordreset.PasswordResetDtos.ForgotPasswordRequest;
import de.tellerstatttonne.backend.auth.passwordreset.PasswordResetDtos.ResetPasswordRequest;
import de.tellerstatttonne.backend.auth.passwordreset.PasswordResetService;
import de.tellerstatttonne.backend.user.User;
import de.tellerstatttonne.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    public AuthController(
        AuthService authService,
        UserRepository userRepository,
        PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.parseLong(authentication.getName());
        return userRepository.findById(userId)
            .map(authService::toDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(Long.parseLong(authentication.getName()), request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiate(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(AuthService.BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(AuthService.BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(de.tellerstatttonne.backend.auth.passwordreset.PasswordResetService.InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidToken(
        de.tellerstatttonne.backend.auth.passwordreset.PasswordResetService.InvalidTokenException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
