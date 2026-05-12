package de.tellerstatttonne.backend.distributionpoint.post;

import de.tellerstatttonne.backend.auth.CurrentUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DistributionPostController {

    private final DistributionPostService service;

    public DistributionPostController(DistributionPostService service) {
        this.service = service;
    }

    @GetMapping("/distribution-posts/{id}")
    public ResponseEntity<DistributionPost> get(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/distribution-posts/{id}/photos")
    public ResponseEntity<DistributionPost> addPhoto(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(service.addPhoto(id, file, CurrentUser.requireId()));
    }

    @GetMapping("/distribution-points/{distributionPointId}/posts")
    public List<DistributionPost> listByPoint(
        @PathVariable Long distributionPointId,
        @RequestParam(value = "status", required = false) DistributionPostStatus status
    ) {
        return service.findByDistributionPoint(distributionPointId, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
