package de.tellerstatttonne.backend.member;

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
public class PhotoStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final String PUBLIC_PREFIX = "/uploads/photos/";

    private final Path photosDir;

    public PhotoStorageService(@Value("${app.uploads.dir:./uploads}") String uploadsDir) {
        this.photosDir = Path.of(uploadsDir, "photos").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.photosDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create uploads directory: " + this.photosDir, e);
        }
    }

    public String store(Long memberId, MultipartFile file, String previousPhotoUrl) {
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

        String filename = memberId + "-" + UUID.randomUUID() + "." + extension;
        Path target = photosDir.resolve(filename).normalize();
        if (!target.startsWith(photosDir)) {
            throw new IllegalArgumentException("invalid filename");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("failed to store file", e);
        }

        deletePreviousIfLocal(previousPhotoUrl);
        return PUBLIC_PREFIX + filename;
    }

    private void deletePreviousIfLocal(String previousPhotoUrl) {
        if (previousPhotoUrl == null || !previousPhotoUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String oldFilename = previousPhotoUrl.substring(PUBLIC_PREFIX.length());
        Path oldPath = photosDir.resolve(oldFilename).normalize();
        if (!oldPath.startsWith(photosDir)) {
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

    public Path getPhotosDir() {
        return photosDir;
    }
}
