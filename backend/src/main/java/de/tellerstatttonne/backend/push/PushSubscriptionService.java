package de.tellerstatttonne.backend.push;

import java.nio.charset.StandardCharsets;
import java.util.List;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionService.class);

    private final PushSubscriptionRepository repository;
    private final PushService pushService;

    public PushSubscriptionService(PushSubscriptionRepository repository, PushService pushService) {
        this.repository = repository;
        this.pushService = pushService;
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
        String body = serialize(payload);
        for (PushSubscriptionEntity sub : subs) {
            send(sub, body);
        }
    }

    @Async
    public void sendToAll(PushPayload payload) {
        List<PushSubscriptionEntity> subs = repository.findAll();
        String body = serialize(payload);
        for (PushSubscriptionEntity sub : subs) {
            send(sub, body);
        }
    }

    private void send(PushSubscriptionEntity entity, String body) {
        try {
            Subscription.Keys keys = new Subscription.Keys(entity.getP256dh(), entity.getAuth());
            Subscription subscription = new Subscription(entity.getEndpoint(), keys);
            Notification notification = new Notification(subscription, body);
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                log.info("Push subscription gone (status={}), removing endpoint={}", status, entity.getEndpoint());
                repository.deleteByEndpoint(entity.getEndpoint());
            } else if (status >= 400) {
                String reason = readBody(response.getEntity());
                log.warn("Push send failed (status={}) for endpoint={} reason={}",
                    status, entity.getEndpoint(), reason);
            }
        } catch (Exception e) {
            log.error("Push send error for endpoint={}: {}", entity.getEndpoint(), e.getMessage(), e);
        }
    }

    private static String readBody(HttpEntity httpEntity) {
        if (httpEntity == null) return "<no body>";
        try {
            String body = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
            return body == null || body.isBlank() ? "<empty>" : body.trim();
        } catch (Exception e) {
            return "<read failed: " + e.getMessage() + ">";
        }
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
