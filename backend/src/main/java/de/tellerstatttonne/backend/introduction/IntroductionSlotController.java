package de.tellerstatttonne.backend.introduction;

import de.tellerstatttonne.backend.auth.CurrentUser;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/introduction-slots")
public class IntroductionSlotController {

    private final IntroductionSlotService service;

    public IntroductionSlotController(IntroductionSlotService service) {
        this.service = service;
    }

    @GetMapping("/available")
    public List<IntroductionSlotDto> available() {
        return service.listAvailable(CurrentUser.requireId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public List<IntroductionSlotDto> listAll() {
        return service.listAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<IntroductionSlotDto> create(@RequestBody IntroductionSlotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(req, CurrentUser.requireId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public IntroductionSlotDto update(@PathVariable Long id, @RequestBody IntroductionSlotRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/book")
    public IntroductionSlotDto book(@PathVariable Long id) {
        return service.book(id, CurrentUser.requireId());
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancel(@PathVariable Long bookingId, Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR")
                || a.getAuthority().equals("ROLE_TEAMLEITER"));
        service.cancelBooking(bookingId, CurrentUser.requireId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{slotId}/confirm-attendance/{userId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<Void> confirmAttendance(@PathVariable Long slotId,
                                                  @PathVariable Long userId) {
        service.confirmAttendance(slotId, userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
