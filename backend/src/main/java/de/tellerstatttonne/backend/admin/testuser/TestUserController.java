package de.tellerstatttonne.backend.admin.testuser;

import de.tellerstatttonne.backend.admin.testuser.TestUserDtos.CreateTestUserRequest;
import de.tellerstatttonne.backend.admin.testuser.TestUserDtos.TestUserDto;
import de.tellerstatttonne.backend.auth.AuthDtos.AuthResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test-users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class TestUserController {

    private final TestUserService service;

    public TestUserController(TestUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestUserDto> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<TestUserDto> create(@RequestBody(required = false) CreateTestUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/impersonate")
    public AuthResponse impersonate(@PathVariable Long id) {
        return service.impersonate(id);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
