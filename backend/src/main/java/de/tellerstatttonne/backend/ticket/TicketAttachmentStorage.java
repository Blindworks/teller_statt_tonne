package de.tellerstatttonne.backend.ticket;

import de.tellerstatttonne.backend.storage.ImageStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TicketAttachmentStorage {

    private static final String SUBDIR = "tickets";
    private static final String PUBLIC_PREFIX = "/uploads/" + SUBDIR + "/";

    private final ImageStorageService imageStorageService;

    public TicketAttachmentStorage(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    public String store(Long ticketId, MultipartFile file) {
        return imageStorageService.store(SUBDIR, ticketId.toString(), file, null);
    }

    public void deleteByUrl(String url) {
        if (url == null || !url.startsWith(PUBLIC_PREFIX)) return;
        String filename = url.substring(PUBLIC_PREFIX.length());
        Path uploadsDir = imageStorageService.getUploadsDir();
        Path subdir = uploadsDir.resolve(SUBDIR).normalize();
        Path target = subdir.resolve(filename).normalize();
        if (!target.startsWith(subdir)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
