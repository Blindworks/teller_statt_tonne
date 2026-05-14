package de.tellerstatttonne.backend.feature;

import de.tellerstatttonne.backend.auth.CurrentUser;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureController {

    private final FeatureService service;

    public FeatureController(FeatureService service) {
        this.service = service;
    }

    // --- Feature-Verwaltung (nur Admin) ---

    @GetMapping("/api/features")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<Feature> list() {
        return service.listAll();
    }

    @PostMapping("/api/features")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Feature> create(@RequestBody FeatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/api/features/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Feature update(@PathVariable Long id, @RequestBody FeatureRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/features/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Role <-> Feature Matrix (nur Admin) ---

    @GetMapping("/api/roles/{roleId}/features")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public RoleFeatureAssignment getForRole(@PathVariable Long roleId) {
        return new RoleFeatureAssignment(service.featureIdsForRole(roleId));
    }

    @PutMapping("/api/roles/{roleId}/features")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public RoleFeatureAssignment setForRole(@PathVariable Long roleId,
                                            @RequestBody RoleFeatureAssignment body) {
        service.setFeaturesForRole(roleId, body.featureIds());
        return new RoleFeatureAssignment(service.featureIdsForRole(roleId));
    }

    // --- Eigene Features (jeder authentifizierte User) ---

    @GetMapping("/api/me/features")
    public Set<String> mine() {
        return service.featureKeysForUser(CurrentUser.requireId());
    }

    // --- Fehlerbehandlung ---

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(FeatureService.AdminRoleLockedException.class)
    public ResponseEntity<String> handleAdminLocked(FeatureService.AdminRoleLockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
