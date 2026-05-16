package de.tellerstatttonne.backend.rescuercard;

import de.tellerstatttonne.backend.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/verify")
public class VerifyController {

    private final JwtService jwtService;
    private final RescuerCardService service;

    public VerifyController(JwtService jwtService, RescuerCardService service) {
        this.jwtService = jwtService;
        this.service = service;
    }

    public record VerifyResponse(
        boolean valid,
        String reason,
        String firstName,
        String lastName,
        String photoUrl,
        boolean hygieneValid,
        String currentPartnerName,
        boolean pickupActive,
        Instant generatedAt
    ) {
        static VerifyResponse invalid(String reason) {
            return new VerifyResponse(false, reason, null, null, null, false, null, false, null);
        }
    }

    @GetMapping("/{token}")
    public ResponseEntity<VerifyResponse> verify(@PathVariable String token) {
        Claims claims;
        try {
            claims = jwtService.parseRescuerCardToken(token);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.ok(VerifyResponse.invalid("expired"));
        } catch (JwtException e) {
            return ResponseEntity.ok(VerifyResponse.invalid("invalid"));
        }

        Long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(VerifyResponse.invalid("invalid"));
        }

        RescuerCardContext ctx;
        try {
            ctx = service.buildContext(userId);
        } catch (Exception e) {
            return ResponseEntity.ok(VerifyResponse.invalid("user-not-found"));
        }

        RescuerCardContext.CurrentPickup pickup = ctx.currentPickup();
        return ResponseEntity.ok(new VerifyResponse(
            true,
            null,
            ctx.firstName(),
            ctx.lastName(),
            ctx.photoUrl(),
            ctx.hygieneValid(),
            pickup != null ? pickup.partnerName() : null,
            pickup != null && pickup.active(),
            ctx.generatedAt()
        ));
    }
}
