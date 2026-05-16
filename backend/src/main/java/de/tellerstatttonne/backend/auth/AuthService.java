package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.auth.AuthDtos.AuthResponse;
import de.tellerstatttonne.backend.auth.AuthDtos.LoginRequest;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import de.tellerstatttonne.backend.user.User;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserMapper;
import de.tellerstatttonne.backend.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ApplicationEventPublisher eventPublisher,
        @Value("${app.jwt.refresh-ttl}") Duration refreshTtl
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.refreshTtl = refreshTtl;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.LOGIN_FAILED)
                .actorEmail(email)
                .message("Login fehlgeschlagen: unbekannte E-Mail")
                .build());
            throw new BadCredentialsException("Invalid credentials");
        }
        if (user.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.LOGIN_FAILED)
                .actor(user.getId(), user.getEmail())
                .target("USER", user.getId())
                .message("Login fehlgeschlagen: falsches Passwort")
                .build());
            throw new BadCredentialsException("Invalid credentials");
        }
        if (user.getStatus() == UserEntity.Status.LOCKED) {
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.LOGIN_FAILED)
                .actor(user.getId(), user.getEmail())
                .target("USER", user.getId())
                .message("Login fehlgeschlagen: Nutzer ist gesperrt")
                .build());
            throw new BadCredentialsException("Account locked");
        }
        AuthResponse response = buildAuthResponse(user);
        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.LOGIN_SUCCESS)
            .actor(user.getId(), user.getEmail())
            .target("USER", user.getId())
            .message("Login erfolgreich")
            .build());
        return response;
    }

    public AuthResponse refresh(String refreshToken) {
        String hash = sha256(refreshToken);
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        UserEntity user = userRepository.findById(stored.getUserId())
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (user.getStatus() == UserEntity.Status.LOCKED) {
            throw new BadCredentialsException("Account locked");
        }

        int deleted = refreshTokenRepository.deleteByIdAndRevokedFalse(stored.getId());
        if (deleted == 0) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        refreshTokenRepository.flush();

        return buildAuthResponse(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.PASSWORD_CHANGED)
            .actor(user.getId(), user.getEmail())
            .target("USER", user.getId())
            .message("Passwort geaendert")
            .build());
    }

    public void logout(String refreshToken) {
        String hash = sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
            UserEntity user = userRepository.findById(t.getUserId()).orElse(null);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.LOGOUT)
                .actor(user != null ? user.getId() : t.getUserId(), user != null ? user.getEmail() : null)
                .target("USER", t.getUserId())
                .message("Logout")
                .build());
        });
    }

    public User toDto(UserEntity entity) {
        return UserMapper.toDto(entity);
    }

    public AuthResponse issueTokens(UserEntity user) {
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = generateRefreshToken();

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash(sha256(refreshToken));
        entity.setUserId(user.getId());
        entity.setExpiresAt(Instant.now().plus(refreshTtl));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtService.getAccessTtl().toSeconds(),
            toDto(user)
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String message) {
            super(message);
        }
    }
}
