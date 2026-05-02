package de.tellerstatttonne.backend.user.availability;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/availabilities")
@PreAuthorize("hasAnyRole('ADMINISTRATOR','BOTSCHAFTER') or principal == #userId.toString()")
public class UserAvailabilityController {

    private final UserAvailabilityService service;

    public UserAvailabilityController(UserAvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserAvailability> list(@PathVariable Long userId) {
        return service.findByUserId(userId);
    }

    @PutMapping
    public List<UserAvailability> replaceAll(
        @PathVariable Long userId,
        @RequestBody List<UserAvailability> items
    ) {
        return service.replaceAll(userId, items);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
