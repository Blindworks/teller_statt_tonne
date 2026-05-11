package de.tellerstatttonne.backend.event;

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
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final ImageStorageService imageStorageService;

    public EventController(EventService service, ImageStorageService imageStorageService) {
        this.service = service;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public List<Event> list(@RequestParam(defaultValue = "active") String scope) {
        return service.findAll(parseScope(scope));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<Event> create(@RequestBody Event dto) {
        Event created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public ResponseEntity<Event> update(@PathVariable Long id, @RequestBody Event dto) {
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
    public ResponseEntity<Event> uploadLogo(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String previousUrl = service.findLogoUrl(id).orElse(null);
        String newUrl = imageStorageService.store("event-logos", id.toString(), file, previousUrl);
        return service.updateLogoUrl(id, newUrl)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static EventService.Scope parseScope(String value) {
        if (value == null) return EventService.Scope.ACTIVE;
        return switch (value.toLowerCase()) {
            case "past" -> EventService.Scope.PAST;
            case "all" -> EventService.Scope.ALL;
            default -> EventService.Scope.ACTIVE;
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
