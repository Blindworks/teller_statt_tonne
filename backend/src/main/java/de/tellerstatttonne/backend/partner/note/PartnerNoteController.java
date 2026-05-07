package de.tellerstatttonne.backend.partner.note;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partners/{partnerId}/notes")
@PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER','RETTER')")
public class PartnerNoteController {

    private final PartnerNoteService service;

    public PartnerNoteController(PartnerNoteService service) {
        this.service = service;
    }

    @GetMapping
    public List<PartnerNote> list(@PathVariable Long partnerId) {
        return service.listForUser(partnerId, CurrentUser.requireId());
    }

    @PostMapping
    public PartnerNote create(
        @PathVariable Long partnerId,
        @Valid @RequestBody CreatePartnerNoteRequest request
    ) {
        return service.create(partnerId, request, CurrentUser.requireId());
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<Void> delete(@PathVariable Long partnerId, @PathVariable Long noteId) {
        boolean ok = service.softDelete(noteId, CurrentUser.requireId());
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ex.getMessage());
    }
}
