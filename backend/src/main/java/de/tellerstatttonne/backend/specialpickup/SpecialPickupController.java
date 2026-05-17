package de.tellerstatttonne.backend.specialpickup;

import de.tellerstatttonne.backend.storage.ImageStorageService;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/special-pickups")
public class SpecialPickupController {

    private final SpecialPickupService service;
    private final ImageStorageService imageStorageService;

    public SpecialPickupController(SpecialPickupService service, ImageStorageService imageStorageService) {
        this.service = service;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public List<SpecialPickup> list(@RequestParam(defaultValue = "active") String scope) {
        return service.findAll(parseScope(scope));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialPickup> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<SpecialPickup> create(@RequestBody SpecialPickup dto) {
        SpecialPickup created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<SpecialPickup> update(@PathVariable Long id, @RequestBody SpecialPickup dto) {
        return service.update(id, dto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<SpecialPickup> uploadLogo(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String previousUrl = service.findLogoUrl(id).orElse(null);
        String newUrl = imageStorageService.store("special-pickup-logos", id.toString(), file, previousUrl);
        return service.updateLogoUrl(id, newUrl)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static SpecialPickupService.Scope parseScope(String value) {
        if (value == null) return SpecialPickupService.Scope.ACTIVE;
        return switch (value.toLowerCase()) {
            case "past" -> SpecialPickupService.Scope.PAST;
            case "all" -> SpecialPickupService.Scope.ALL;
            default -> SpecialPickupService.Scope.ACTIVE;
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
