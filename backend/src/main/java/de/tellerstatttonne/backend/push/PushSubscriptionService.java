package de.tellerstatttonne.backend.push;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionService.class);
    private static final int DEFAULT_TTL_SECONDS = 60 * 60 * 24; // 24h

    private final PushSubscriptionRepository repository;
    private final WebPushSender sender;

    public PushSubscriptionService(PushSubscriptionRepository repository, WebPushSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @Transactional
    public void subscribe(Long userId, PushSubscriptionRequest req) {
        repository.findByEndpoint(req.endpoint()).ifPresentOrElse(existing -> {
            existing.setUserId(userId);
            existing.setP256dh(req.p256dh());
            existing.setAuth(req.auth());
            existing.setUserAgent(req.userAgent());
        }, () -> repository.save(new PushSubscriptionEntity(
            userId, req.endpoint(), req.p256dh(), req.auth(), req.userAgent()
        )));
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        repository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    @Async
    public void sendToUser(Long userId, PushPayload payload) {
        List<PushSubscriptionEntity> subs = repository.findAllByUserId(userId);
        byte[] body = serialize(payload).getBytes(StandardCharsets.UTF_8);
        for (PushSubscriptionEntity sub : subs) {
            send(sub, body);
        }
    }

    @Async
    public void sendToAll(PushPayload payload) {
        List<PushSubscriptionEntity> subs = repository.findAll();
        byte[] body = serialize(payload).getBytes(StandardCharsets.UTF_8);
        for (PushSubscriptionEntity sub : subs) {
            send(sub, body);
        }
    }

    private void send(PushSubscriptionEntity entity, byte[] body) {
        if (!sender.isEnabled()) {
            log.warn("Web Push übersprungen: kein VAPID-Key konfiguriert.");
            return;
        }
        try {
            WebPushSender.Result result = sender.send(
                entity.getEndpoint(),
                entity.getP256dh(),
                entity.getAuth(),
                body,
                DEFAULT_TTL_SECONDS
            );
            int status = result.status();
            if (status == 404 || status == 410) {
                log.info("Push subscription gone (status={}), removing endpoint={}", status, entity.getEndpoint());
                repository.deleteByEndpoint(entity.getEndpoint());
            } else if (status >= 400) {
                log.warn("Push send failed (status={}) for endpoint={} reason={}",
                    status, entity.getEndpoint(), shortBody(result.body()));
            }
        } catch (Exception e) {
            log.error("Push send error for endpoint={}: {}", entity.getEndpoint(), e.getMessage(), e);
        }
    }

    private static String shortBody(String body) {
        if (body == null) return "<no body>";
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return "<empty>";
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "…" : trimmed;
    }

    private String serialize(PushPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"notification\":{");
        sb.append("\"title\":").append(jsonString(payload.title()));
        sb.append(",\"body\":").append(jsonString(payload.body()));
        sb.append(",\"icon\":\"/icons/icon-192.png\"");
        sb.append(",\"badge\":\"/icons/icon-192.png\"");
        if (payload.tag() != null) {
            sb.append(",\"tag\":").append(jsonString(payload.tag()));
        }
        if (payload.url() != null) {
            sb.append(",\"data\":{\"url\":").append(jsonString(payload.url()));
            sb.append(",\"onActionClick\":{\"default\":{\"operation\":\"openWindow\",\"url\":")
              .append(jsonString(payload.url())).append("}}}");
        } else {
            sb.append(",\"data\":{}");
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
