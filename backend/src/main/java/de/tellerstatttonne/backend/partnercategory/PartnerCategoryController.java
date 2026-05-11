package de.tellerstatttonne.backend.partnercategory;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partner-categories")
public class PartnerCategoryController {

    private final PartnerCategoryService service;

    public PartnerCategoryController(PartnerCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<PartnerCategory> listActive() {
        return service.findActive();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public List<PartnerCategory> listAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<PartnerCategory> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<PartnerCategory> create(@RequestBody PartnerCategory dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<PartnerCategory> update(@PathVariable Long id, @RequestBody PartnerCategory dto) {
        return service.update(id, dto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return switch (service.delete(id)) {
            case DELETED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case IN_USE -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Kategorie wird noch von Betrieben verwendet und kann nicht gelöscht werden.");
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
