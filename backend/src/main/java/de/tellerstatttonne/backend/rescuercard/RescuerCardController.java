package de.tellerstatttonne.backend.rescuercard;

import de.tellerstatttonne.backend.auth.JwtService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rescuer-card")
@PreAuthorize("hasRole('RETTER') or hasRole('ADMINISTRATOR') or hasRole('TEAMLEITER')")
public class RescuerCardController {

    private final RescuerCardService service;
    private final JwtService jwtService;

    public RescuerCardController(RescuerCardService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    public record TokenResponse(String token, Instant expiresAt) {}

    @GetMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.parseLong(authentication.getName());
        String token = jwtService.issueRescuerCardToken(userId);
        Instant expiresAt = Instant.now().plus(JwtService.RESCUER_CARD_TTL);
        return ResponseEntity.ok(new TokenResponse(token, expiresAt));
    }

    @GetMapping("/context")
    public ResponseEntity<RescuerCardContext> context(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(service.buildContext(userId));
    }
}
