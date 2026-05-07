package de.tellerstatttonne.backend.partner.application;

import de.tellerstatttonne.backend.auth.CurrentUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PartnerApplicationController {

    private final PartnerApplicationService service;

    public PartnerApplicationController(PartnerApplicationService service) {
        this.service = service;
    }

    @PostMapping("/partner-applications")
    @PreAuthorize("hasRole('RETTER') or hasRole('NEW_MEMBER') or hasRole('ADMINISTRATOR') or hasRole('TEAMLEITER')")
    public PartnerApplicationDto apply(@Valid @RequestBody CreatePartnerApplicationRequest request) {
        return service.apply(request.partnerId(), CurrentUser.requireId(), request.message());
    }

    @DeleteMapping("/partner-applications/{id}")
    public PartnerApplicationDto withdraw(@PathVariable Long id) {
        return service.withdraw(id, CurrentUser.requireId());
    }

    @PostMapping("/partner-applications/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public PartnerApplicationDto approve(@PathVariable Long id) {
        return service.approve(id, CurrentUser.requireId());
    }

    @PostMapping("/partner-applications/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public PartnerApplicationDto reject(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) RejectApplicationRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        return service.reject(id, CurrentUser.requireId(), reason);
    }

    @GetMapping("/partners/{partnerId}/applications")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public List<PartnerApplicationDto> listForPartner(
        @PathVariable Long partnerId,
        @RequestParam(required = false) ApplicationStatus status
    ) {
        return service.listForPartner(partnerId, status);
    }

    @GetMapping("/users/me/partner-applications")
    public List<PartnerApplicationDto> listMine() {
        return service.listForUser(CurrentUser.requireId());
    }

    @GetMapping("/partner-applications/pending-count")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public long pendingCount() {
        return service.pendingCount();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ex.getMessage());
    }
}
