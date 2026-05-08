package de.tellerstatttonne.backend.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentStorageService {

    public static final String CERTIFICATES_SUBDIR = "certificates";

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf", "image/jpeg", "image/png", "image/webp"
    );

    private final Path uploadsDir;

    public DocumentStorageService(@Value("${app.uploads.dir:./uploads}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create uploads directory: " + this.uploadsDir, e);
        }
    }

    public StoredDocument store(String subdir, String idPrefix, MultipartFile file, String previousRelativePath) {
        validateSubdir(subdir);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("file too large (max 10 MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("unsupported content type: " + contentType);
        }
        String extension = extractExtension(file.getOriginalFilename(), contentType);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported file extension: " + extension);
        }

        Path targetDir = uploadsDir.resolve(subdir).normalize();
        if (!targetDir.startsWith(uploadsDir)) {
            throw new IllegalArgumentException("invalid subdir");
        }
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create directory: " + targetDir, e);
        }

        String safePrefix = idPrefix == null ? "" : idPrefix.replaceAll("[^A-Za-z0-9_-]", "");
        String filename = (safePrefix.isEmpty() ? "" : safePrefix + "-") + UUID.randomUUID() + "." + extension;
        Path target = targetDir.resolve(filename).normalize();
        if (!target.startsWith(targetDir)) {
            throw new IllegalArgumentException("invalid filename");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("failed to store file", e);
        }

        delete(previousRelativePath);

        String relativePath = subdir + "/" + filename;
        return new StoredDocument(relativePath, contentType.toLowerCase(), file.getSize(),
            file.getOriginalFilename());
    }

    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
        Path resolved = uploadsDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadsDir)) {
            throw new IllegalArgumentException("invalid path");
        }
        return resolved;
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path resolved = resolve(relativePath);
            Files.deleteIfExists(resolved);
        } catch (IllegalArgumentException ignored) {
            // best-effort
        } catch (IOException ignored) {
            // best-effort
        }
    }

    private void validateSubdir(String subdir) {
        if (subdir == null || subdir.isBlank()
            || subdir.contains("/") || subdir.contains("\\") || subdir.contains("..")) {
            throw new IllegalArgumentException("invalid subdir");
        }
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot + 1).toLowerCase();
                if (ext.equals("jpeg")) return "jpg";
                return ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }

    public record StoredDocument(String relativePath, String mimeType, long sizeBytes, String originalFilename) {}
}
