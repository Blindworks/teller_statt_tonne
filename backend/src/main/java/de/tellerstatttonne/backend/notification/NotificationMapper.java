package de.tellerstatttonne.backend.notification;

final class NotificationMapper {

    private NotificationMapper() {}

    static Notification toDto(NotificationEntity e) {
        return new Notification(
            e.getId(),
            e.getType(),
            e.getTitle(),
            e.getBody(),
            e.getRelatedPickupId(),
            e.getRelatedPartnerId(),
            e.getActorUserId(),
            e.getCreatedAt(),
            e.getReadAt()
        );
    }
}
