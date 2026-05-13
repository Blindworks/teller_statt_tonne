package de.tellerstatttonne.backend.notification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

@Service
public class NotificationStreamService {

    private static final Logger log = LoggerFactory.getLogger(NotificationStreamService.class);
    private static final long EMITTER_TIMEOUT_MS = 0L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByUserId.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        trySend(userId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    public void pushNotification(Long userId, Notification notification) {
        send(userId, "notification", notification);
    }

    public void pushUnreadCount(Long userId, long count) {
        send(userId, "unread-count", Map.of("count", count));
    }

    private void send(Long userId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) return;
        for (SseEmitter emitter : emitters) {
            trySend(userId, emitter, SseEmitter.event().name(eventName).data(payload));
        }
    }

    @Scheduled(fixedDelay = 25_000L)
    void heartbeat() {
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emittersByUserId.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                trySend(entry.getKey(), emitter, SseEmitter.event().name("ping").data(""));
            }
        }
    }

    private void trySend(Long userId, SseEmitter emitter, SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception ex) {
            if (isClientDisconnect(ex)) {
                log.debug("SSE-Client für User {} getrennt, entferne Emitter: {}", userId, ex.toString());
            } else {
                log.warn("SSE-Send für User {} fehlgeschlagen, entferne Emitter", userId, ex);
            }
            remove(userId, emitter);
        }
    }

    private static boolean isClientDisconnect(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof java.io.IOException
                || t instanceof IllegalStateException
                || t instanceof AsyncRequestNotUsableException
                || t instanceof ClientAbortException) {
                return true;
            }
        }
        return false;
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emittersByUserId.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emittersByUserId.remove(userId);
        }
    }

    int activeEmitterCount(Long userId) {
        CopyOnWriteArrayList<SseEmitter> list = emittersByUserId.get(userId);
        return list == null ? 0 : list.size();
    }
}
