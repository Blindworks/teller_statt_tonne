package de.tellerstatttonne.backend.push;

import de.tellerstatttonne.backend.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushSubscriptionService service;
    private final WebPushConfig webPushConfig;

    public PushController(PushSubscriptionService service, WebPushConfig webPushConfig) {
        this.service = service;
        this.webPushConfig = webPushConfig;
    }

    @GetMapping(value = "/vapid-public-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> vapidPublicKey() {
        String key = webPushConfig.getPublicKey();
        if (key == null || key.isBlank()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(key);
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest req) {
        service.subscribe(CurrentUser.requireId(), req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest req) {
        service.unsubscribe(CurrentUser.requireId(), req.endpoint());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Void> sendTest() {
        PushPayload payload = PushPayload.of(
            "Test-Benachrichtigung",
            "Wenn du das siehst, funktioniert Web Push 🎉",
            "/profil"
        );
        service.sendToUser(CurrentUser.requireId(), payload);
        return ResponseEntity.noContent().build();
    }
}
