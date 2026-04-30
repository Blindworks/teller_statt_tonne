package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.auth.AuthDtos.AuthResponse;
import de.tellerstatttonne.backend.auth.AuthDtos.LoginRequest;
import de.tellerstatttonne.backend.auth.AuthDtos.RegisterRequest;
import de.tellerstatttonne.backend.member.Member;
import de.tellerstatttonne.backend.member.MemberService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
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
    private final MemberService memberService;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        MemberService memberService,
        @Value("${app.jwt.refresh-ttl}") Duration refreshTtl
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.memberService = memberService;
        this.refreshTtl = refreshTtl;
    }

    public UserEntity ensureMemberLink(UserEntity user) {
        if (user.getMemberId() != null) {
            return user;
        }
        Member created = memberService.createForUser(user.getEmail());
        user.setMemberId(created.id());
        return userRepository.save(user);
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        UserEntity entity = new UserEntity();
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRole(Role.USER);
        UserEntity saved = userRepository.save(entity);
        saved = ensureMemberLink(saved);
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        user = ensureMemberLink(user);
        return buildAuthResponse(user);
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

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

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
    }

    public void logout(String refreshToken) {
        String hash = sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    public User toDto(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getRole(), entity.getMemberId());
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
