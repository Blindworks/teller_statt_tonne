package de.tellerstatttonne.backend.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final int DEFAULT_LIMIT = 50;

    private final NotificationRepository repository;
    private final NotificationStreamService stream;

    public NotificationService(NotificationRepository repository, NotificationStreamService stream) {
        this.repository = repository;
        this.stream = stream;
    }

    public void create(Collection<Long> recipientUserIds,
                       NotificationType type,
                       String title,
                       String body,
                       Long relatedPickupId,
                       Long relatedPartnerId,
                       Long actorUserId) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) return;
        Set<Long> unique = new HashSet<>(recipientUserIds);
        unique.remove(null);
        if (actorUserId != null) unique.remove(actorUserId);
        if (unique.isEmpty()) return;
        Instant now = Instant.now();
        for (Long userId : unique) {
            NotificationEntity entity = new NotificationEntity();
            entity.setRecipientUserId(userId);
            entity.setType(type);
            entity.setTitle(title);
            entity.setBody(body);
            entity.setRelatedPickupId(relatedPickupId);
            entity.setRelatedPartnerId(relatedPartnerId);
            entity.setActorUserId(actorUserId);
            entity.setCreatedAt(now);
            NotificationEntity saved = repository.save(entity);
            Notification dto = NotificationMapper.toDto(saved);
            stream.pushNotification(userId, dto);
            stream.pushUnreadCount(userId, repository.countByRecipientUserIdAndReadAtIsNull(userId));
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> list(Long userId, int limit) {
        int capped = Math.max(1, Math.min(limit, DEFAULT_LIMIT));
        return repository.findByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, capped))
            .stream()
            .map(NotificationMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    public boolean markRead(Long userId, Long notificationId) {
        return repository.findByIdAndRecipientUserId(notificationId, userId).map(entity -> {
            if (entity.getReadAt() == null) {
                entity.setReadAt(Instant.now());
                repository.save(entity);
                stream.pushUnreadCount(userId, repository.countByRecipientUserIdAndReadAtIsNull(userId));
            }
            return true;
        }).orElse(false);
    }

    public void markAllRead(Long userId) {
        Instant now = Instant.now();
        List<NotificationEntity> unread = repository.findByRecipientUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(0, 500));
        boolean changed = false;
        for (NotificationEntity entity : unread) {
            if (entity.getReadAt() == null) {
                entity.setReadAt(now);
                changed = true;
            }
        }
        if (changed) {
            repository.saveAll(unread);
            stream.pushUnreadCount(userId, repository.countByRecipientUserIdAndReadAtIsNull(userId));
        }
    }
}
