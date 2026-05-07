package de.tellerstatttonne.backend.notification;

import de.tellerstatttonne.backend.auth.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationStreamService stream;

    public NotificationController(NotificationService service, NotificationStreamService stream) {
        this.service = service;
        this.stream = stream;
    }

    @GetMapping
    public List<Notification> list(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return service.list(CurrentUser.requireId(), limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(CurrentUser.requireId()));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = CurrentUser.requireId();
        SseEmitter emitter = stream.register(userId);
        stream.pushUnreadCount(userId, service.unreadCount(userId));
        return emitter;
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        boolean ok = service.markRead(CurrentUser.requireId(), id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        service.markAllRead(CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }
}
