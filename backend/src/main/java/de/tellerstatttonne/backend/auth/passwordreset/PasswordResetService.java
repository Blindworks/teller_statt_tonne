package de.tellerstatttonne.backend.auth.passwordreset;

import de.tellerstatttonne.backend.auth.RefreshTokenRepository;
import de.tellerstatttonne.backend.config.AppProperties;
import de.tellerstatttonne.backend.mail.MailService;
import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        MailService mailService,
        AppProperties appProperties,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.appProperties = appProperties;
        this.eventPublisher = eventPublisher;
    }

    public enum MailKind { RESET, INVITATION }

    public void initiate(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        Optional<UserEntity> userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            log.info("Passwort-Reset angefordert fuer unbekannte Mail (still ignoriert)");
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.PASSWORD_RESET_REQUESTED)
                .actorEmail(normalized)
                .message("Passwort-Reset angefordert fuer unbekannte Mail")
                .build());
            return;
        }
        sendTokenMail(userOpt.get(), MailKind.RESET);
    }

    /**
     * Sends an invitation mail to a freshly created user that contains a link
     * to set the initial password. Reuses the password-reset token machinery.
     */
    public void sendInvitation(UserEntity user) {
        sendTokenMail(user, MailKind.INVITATION);
    }

    private void sendTokenMail(UserEntity user, MailKind kind) {
        if (kind == MailKind.RESET) {
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.PASSWORD_RESET_REQUESTED)
                .actor(user.getId(), user.getEmail())
                .target("USER", user.getId())
                .message("Passwort-Reset angefordert")
                .build());
        } else {
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.USER_INVITATION_SENT)
                .actor(user.getId(), user.getEmail())
                .target("USER", user.getId())
                .message("Einladungs-Mail gesendet")
                .build());
        }

        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateRawToken();
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setTokenHash(sha256(rawToken));
        entity.setUserId(user.getId());
        entity.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenRepository.save(entity);

        String resetUrl = buildResetUrl(rawToken);
        String subject = kind == MailKind.INVITATION
            ? "Willkommen bei Teller statt Tonne — Passwort vergeben"
            : "Passwort zuruecksetzen";
        String html = renderHtml(user.getFirstName(), resetUrl, kind);
        String plain = renderPlain(user.getFirstName(), resetUrl, kind);
        mailService.sendHtml(user.getEmail(), subject, html, plain);
    }

    public void reset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Ungueltiger Token");
        }
        String hash = sha256(rawToken);
        PasswordResetTokenEntity token = tokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new InvalidTokenException("Ungueltiger oder abgelaufener Token"));
        if (token.getUsedAt() != null) {
            throw new InvalidTokenException("Token wurde bereits verwendet");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token ist abgelaufen");
        }

        UserEntity user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new InvalidTokenException("Ungueltiger Token"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Passwort fuer User {} via Reset-Token zurueckgesetzt", user.getId());
        eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.PASSWORD_RESET_COMPLETED)
            .actor(user.getId(), user.getEmail())
            .target("USER", user.getId())
            .message("Passwort via Reset-Token zurueckgesetzt")
            .build());
    }

    private String buildResetUrl(String rawToken) {
        String base = appProperties.frontend().baseUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/reset-password/" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private String renderHtml(String firstName, String resetUrl, MailKind kind) {
        String safeName = firstName == null ? "" : firstName;
        String intro;
        String cta;
        String footer;
        if (kind == MailKind.INVITATION) {
            intro = "<p>willkommen bei <strong>Teller statt Tonne</strong>! Ein Konto wurde fuer dich angelegt.</p>"
                + "<p>Damit du dich anmelden kannst, vergib bitte ueber den folgenden Link innerhalb von 30 Minuten dein Passwort:</p>";
            cta = "Passwort vergeben";
            footer = "<p>Sollte der Link bereits abgelaufen sein, kannst du jederzeit ueber \"Passwort vergessen\" auf der Login-Seite ein neues Passwort anfordern.</p>";
        } else {
            intro = "<p>du hast eine Zuruecksetzung deines Passworts fuer <strong>Teller statt Tonne</strong> angefordert.</p>"
                + "<p>Ueber den folgenden Link kannst du innerhalb von 30 Minuten ein neues Passwort vergeben:</p>";
            cta = "Passwort zuruecksetzen";
            footer = "<p>Falls du diese Anfrage nicht gestellt hast, kannst du diese Mail ignorieren — dein Passwort bleibt unveraendert.</p>";
        }
        return "<!DOCTYPE html><html><body style=\"font-family:Arial,sans-serif;color:#222;\">"
            + "<p>Hallo " + escape(safeName) + ",</p>"
            + intro
            + "<p><a href=\"" + resetUrl + "\" style=\"display:inline-block;padding:10px 16px;"
            + "background:#4caf50;color:#fff;text-decoration:none;border-radius:6px;\">" + cta + "</a></p>"
            + "<p>Wenn der Button nicht funktioniert, kopiere diesen Link in deinen Browser:<br/>"
            + "<a href=\"" + resetUrl + "\">" + resetUrl + "</a></p>"
            + footer
            + "<p>Viele Gruesse<br/>Dein Teller-statt-Tonne-Team</p>"
            + "</body></html>";
    }

    private String renderPlain(String firstName, String resetUrl, MailKind kind) {
        String safeName = firstName == null ? "" : firstName;
        if (kind == MailKind.INVITATION) {
            return "Hallo " + safeName + ",\n\n"
                + "willkommen bei Teller statt Tonne! Ein Konto wurde fuer dich angelegt.\n\n"
                + "Damit du dich anmelden kannst, vergib bitte ueber den folgenden Link innerhalb von 30 Minuten dein Passwort:\n"
                + resetUrl + "\n\n"
                + "Sollte der Link bereits abgelaufen sein, kannst du jederzeit ueber \"Passwort vergessen\" auf der Login-Seite ein neues Passwort anfordern.\n\n"
                + "Viele Gruesse\n"
                + "Dein Teller-statt-Tonne-Team\n";
        }
        return "Hallo " + safeName + ",\n\n"
            + "du hast eine Zuruecksetzung deines Passworts fuer Teller statt Tonne angefordert.\n\n"
            + "Ueber den folgenden Link kannst du innerhalb von 30 Minuten ein neues Passwort vergeben:\n"
            + resetUrl + "\n\n"
            + "Falls du diese Anfrage nicht gestellt hast, kannst du diese Mail ignorieren - dein Passwort bleibt unveraendert.\n\n"
            + "Viele Gruesse\n"
            + "Dein Teller-statt-Tonne-Team\n";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
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

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
