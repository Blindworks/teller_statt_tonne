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
public class ImageStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final String UPLOADS_PUBLIC_PREFIX = "/uploads/";

    private final Path uploadsDir;

    public ImageStorageService(@Value("${app.uploads.dir:./uploads}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create uploads directory: " + this.uploadsDir, e);
        }
    }

    public String store(String subdir, String idPrefix, MultipartFile file, String previousUrl) {
        if (subdir == null || subdir.isBlank() || subdir.contains("/") || subdir.contains("\\") || subdir.contains("..")) {
            throw new IllegalArgumentException("invalid subdir");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("file too large (max 5 MB)");
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

        deletePreviousIfLocal(subdir, previousUrl);
        return UPLOADS_PUBLIC_PREFIX + subdir + "/" + filename;
    }

    private void deletePreviousIfLocal(String subdir, String previousUrl) {
        String prefix = UPLOADS_PUBLIC_PREFIX + subdir + "/";
        if (previousUrl == null || !previousUrl.startsWith(prefix)) {
            return;
        }
        String oldFilename = previousUrl.substring(prefix.length());
        Path subdirPath = uploadsDir.resolve(subdir).normalize();
        Path oldPath = subdirPath.resolve(oldFilename).normalize();
        if (!oldPath.startsWith(subdirPath)) {
            return;
        }
        try {
            Files.deleteIfExists(oldPath);
        } catch (IOException ignored) {
            // best-effort cleanup
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
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }
}
