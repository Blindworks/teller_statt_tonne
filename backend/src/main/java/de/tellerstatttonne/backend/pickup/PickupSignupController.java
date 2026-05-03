package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickups/{pickupId}/signup")
@PreAuthorize("hasRole('RETTER')")
public class PickupSignupController {

    private final PickupSignupService service;

    public PickupSignupController(PickupSignupService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> signup(@PathVariable Long pickupId) {
        return toResponse(service.signup(pickupId, CurrentUser.requireId()));
    }

    @DeleteMapping
    public ResponseEntity<Void> unassign(@PathVariable Long pickupId) {
        return toResponse(service.unassign(pickupId, CurrentUser.requireId()));
    }

    private static ResponseEntity<Void> toResponse(PickupSignupService.Result result) {
        return switch (result) {
            case OK -> ResponseEntity.noContent().build();
            case PICKUP_NOT_FOUND, USER_NOT_FOUND, NOT_ASSIGNED -> ResponseEntity.notFound().build();
            case NOT_MEMBER -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case CAPACITY_FULL -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case PICKUP_PAST -> ResponseEntity.status(HttpStatus.GONE).build();
        };
    }
}
