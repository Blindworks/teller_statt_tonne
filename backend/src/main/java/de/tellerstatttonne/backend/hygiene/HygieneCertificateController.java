package de.tellerstatttonne.backend.hygiene;

import de.tellerstatttonne.backend.auth.CurrentUser;
import de.tellerstatttonne.backend.storage.DocumentStorageService;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class HygieneCertificateController {

    private final HygieneCertificateService service;
    private final DocumentStorageService storage;

    public HygieneCertificateController(HygieneCertificateService service, DocumentStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @PostMapping("/users/{userId}/hygiene-certificate")
    public ResponseEntity<HygieneCertificateDto> submit(
        @PathVariable Long userId,
        @RequestPart("file") MultipartFile file,
        @RequestParam("issuedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate
    ) {
        Long requesterId = CurrentUser.requireId();
        if (!userId.equals(requesterId)) {
            throw new AccessDeniedException("only owner can upload certificate");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.submit(userId, file, issuedDate));
    }

    @GetMapping("/users/{userId}/hygiene-certificate")
    public ResponseEntity<HygieneCertificateDto> get(@PathVariable Long userId) {
        if (!service.canAccess(userId, CurrentUser.requireId())) {
            throw new AccessDeniedException("not allowed");
        }
        return service.findByUserId(userId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/hygiene-certificate/file")
    public ResponseEntity<Resource> getFile(@PathVariable Long userId) throws IOException {
        if (!service.canAccess(userId, CurrentUser.requireId())) {
            throw new AccessDeniedException("not allowed");
        }
        HygieneCertificateEntity entity = service.loadEntityForFile(userId)
            .orElseThrow(() -> new EntityNotFoundException("no certificate for user: " + userId));

        Path path = storage.resolve(entity.getFileUrl());
        if (!Files.exists(path)) {
            throw new EntityNotFoundException("file missing on disk");
        }
        Resource resource = new FileSystemResource(path);
        String filename = entity.getOriginalFilename() != null ? entity.getOriginalFilename() : "zertifikat";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(entity.getMimeType()))
            .contentLength(Files.size(path))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeFilename(filename) + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .body(resource);
    }

    @GetMapping("/hygiene-certificates")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public List<HygieneCertificateDto> list(@RequestParam(required = false) HygieneCertificateStatus status) {
        return service.listByStatus(status);
    }

    @GetMapping("/hygiene-certificates/pending-count")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public long pendingCount() {
        return service.pendingCount();
    }

    @PostMapping("/hygiene-certificates/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public HygieneCertificateDto approve(@PathVariable Long id) {
        return service.approve(id, CurrentUser.requireId());
    }

    @PostMapping("/hygiene-certificates/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','TEAMLEITER')")
    public HygieneCertificateDto reject(@PathVariable Long id, @RequestBody RejectCertificateRequest request) {
        String reason = request != null ? request.reason() : null;
        return service.reject(id, CurrentUser.requireId(), reason);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\"\\r\\n]", "_");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
