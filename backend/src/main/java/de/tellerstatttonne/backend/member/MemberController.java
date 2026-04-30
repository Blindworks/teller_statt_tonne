package de.tellerstatttonne.backend.member;

import de.tellerstatttonne.backend.auth.UserRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService service;
    private final PhotoStorageService photoStorageService;
    private final UserRepository userRepository;

    public MemberController(
        MemberService service,
        PhotoStorageService photoStorageService,
        UserRepository userRepository
    ) {
        this.service = service;
        this.photoStorageService = photoStorageService;
        this.userRepository = userRepository;
    }

    public record RoleOption(MemberRole value, String label) {}

    @GetMapping
    public List<Member> list(
        @RequestParam(required = false) MemberRole role,
        @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly,
        @RequestParam(name = "q", required = false) String search
    ) {
        return service.findAll(role, activeOnly, search);
    }

    @GetMapping("/roles")
    public List<RoleOption> roles() {
        return Arrays.stream(MemberRole.values())
            .map(r -> new RoleOption(r, r.getLabel()))
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Member> create(@RequestBody Member member) {
        Member created = service.create(member);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Member> update(@PathVariable Long id, @RequestBody Member member) {
        return service.update(id, member)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<Member> uploadPhoto(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file,
        Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isAuthorizedForMember(authentication, id)) {
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

    private boolean isAuthorizedForMember(Authentication authentication, Long memberId) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;
        Long userId = Long.parseLong(authentication.getName());
        return userRepository.findById(userId)
            .map(u -> memberId.equals(u.getMemberId()))
            .orElse(false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
