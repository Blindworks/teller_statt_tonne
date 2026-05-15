package de.tellerstatttonne.backend.ticket;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.ticket.dto.Ticket;
import de.tellerstatttonne.backend.ticket.dto.TicketComment;
import de.tellerstatttonne.backend.ticket.dto.TicketCommentRequest;
import de.tellerstatttonne.backend.ticket.dto.TicketCreateRequest;
import de.tellerstatttonne.backend.ticket.dto.TicketStatusRequest;
import de.tellerstatttonne.backend.ticket.dto.TicketSummary;
import de.tellerstatttonne.backend.ticket.dto.TicketUpdateRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<TicketSummary> list(
        @RequestParam(value = "status", required = false) TicketStatus status,
        @RequestParam(value = "category", required = false) TicketCategory category
    ) {
        return service.findAll(status, category);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Ticket> create(@RequestBody TicketCreateRequest request) {
        Ticket created = service.create(request, CurrentUser.requireId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> update(@PathVariable Long id, @RequestBody TicketUpdateRequest request) {
        return service.update(id, request, CurrentUser.requireId())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Ticket> updateStatus(
        @PathVariable Long id,
        @RequestBody TicketStatusRequest request
    ) {
        return service.updateStatus(id, request, CurrentUser.requireId())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id, CurrentUser.requireId())
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<Ticket> addAttachment(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        return service.addAttachment(id, file, CurrentUser.requireId())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
        @PathVariable Long id,
        @PathVariable Long attachmentId
    ) {
        return service.deleteAttachment(id, attachmentId, CurrentUser.requireId())
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketComment> addComment(
        @PathVariable Long id,
        @RequestBody TicketCommentRequest request
    ) {
        return service.addComment(id, request, CurrentUser.requireId())
            .map(c -> ResponseEntity.status(HttpStatus.CREATED).body(c))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable Long id,
        @PathVariable Long commentId
    ) {
        return service.deleteComment(id, commentId, CurrentUser.requireId())
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
