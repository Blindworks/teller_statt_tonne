package de.tellerstatttonne.backend.settings;

import de.tellerstatttonne.backend.auth.CurrentUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class SystemSettingController {

    private final SystemSettingService service;

    public SystemSettingController(SystemSettingService service) {
        this.service = service;
    }

    @GetMapping
    public List<SystemSettingDto> list() {
        return service.findAll().stream().map(SystemSettingDto::from).toList();
    }

    @PutMapping("/{key}")
    public ResponseEntity<SystemSettingDto> update(@PathVariable String key,
                                                   @RequestBody UpdateRequest body) {
        if (body == null || body.value() == null) {
            return ResponseEntity.badRequest().build();
        }
        SystemSettingEntity saved = service.set(key, body.value(), CurrentUser.requireId());
        return ResponseEntity.ok(SystemSettingDto.from(saved));
    }

    public record UpdateRequest(String value) {}
}
