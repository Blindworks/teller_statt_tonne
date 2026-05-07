package de.tellerstatttonne.backend.user;

import de.tellerstatttonne.backend.role.Role;
import de.tellerstatttonne.backend.role.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final RoleService roleService;
    private final PhotoStorageService photoStorageService;

    public UserController(UserService service, RoleService roleService,
                          PhotoStorageService photoStorageService) {
        this.service = service;
        this.roleService = roleService;
        this.photoStorageService = photoStorageService;
    }

    public record RoleOption(String value, String label) {}

    @GetMapping
    public List<User> list(
        @RequestParam(required = false) String role,
        @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
        @RequestParam(name = "q", required = false) String search
    ) {
        return service.findAll(role, activeOnly, search);
    }

    @GetMapping("/roles")
    public List<RoleOption> roles() {
        return roleService.list(false).stream()
            .map(r -> new RoleOption(r.name(), r.label()))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','BOTSCHAFTER')")
    public ResponseEntity<User> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adminCreate(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','BOTSCHAFTER')")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return service.update(id, user)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','BOTSCHAFTER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<User> uploadPhoto(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file,
        Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isAuthorizedForUser(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String previousUrl = service.findPhotoUrl(id).orElse(null);
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String newUrl = photoStorageService.store(id, file, previousUrl);
        return service.updatePhotoUrl(id, newUrl)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAuthorizedForUser(Authentication authentication, Long userId) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN_ROLE_NAME));
        if (isAdmin) return true;
        try {
            return userId.equals(Long.parseLong(authentication.getName()));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
