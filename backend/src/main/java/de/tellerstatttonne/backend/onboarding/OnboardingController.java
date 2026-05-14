package de.tellerstatttonne.backend.onboarding;

import de.tellerstatttonne.backend.auth.CurrentUser;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    public record TestPickupRequest(boolean completed) {}

    @GetMapping("/onboarding/me")
    public OnboardingStatusDto myStatus() {
        return service.getStatus(CurrentUser.requireId());
    }

    @GetMapping("/onboarding/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public OnboardingStatusDto userStatus(@PathVariable Long userId) {
        return service.getStatus(userId);
    }

    @PostMapping("/users/{userId}/agreement")
    public OnboardingStatusDto uploadAgreement(@PathVariable Long userId,
                                               @RequestPart("file") MultipartFile file) {
        Long requesterId = CurrentUser.requireId();
        if (!userId.equals(requesterId)) {
            throw new AccessDeniedException("only owner can upload agreement");
        }
        return service.uploadAgreement(userId, file);
    }

    @PatchMapping("/users/{userId}/test-pickup")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public OnboardingStatusDto setTestPickup(@PathVariable Long userId,
                                             @RequestBody TestPickupRequest req) {
        return service.setTestPickupCompleted(userId, req.completed());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
