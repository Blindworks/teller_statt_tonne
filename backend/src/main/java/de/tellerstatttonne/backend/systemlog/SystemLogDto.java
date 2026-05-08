package de.tellerstatttonne.backend.systemlog;

import java.time.Instant;

public record SystemLogDto(
    Long id,
    Instant createdAt,
    SystemLogEventType eventType,
    SystemLogSeverity severity,
    SystemLogCategory category,
    Long actorUserId,
    String actorEmail,
    String targetType,
    Long targetId,
    String message,
    String details,
    String ipAddress,
    String userAgent
) {
    public static SystemLogDto from(SystemLogEntity e) {
        return new SystemLogDto(
            e.getId(),
            e.getCreatedAt(),
            e.getEventType(),
            e.getSeverity(),
            e.getCategory(),
            e.getActorUserId(),
            e.getActorEmail(),
            e.getTargetType(),
            e.getTargetId(),
            e.getMessage(),
            e.getDetails(),
            e.getIpAddress(),
            e.getUserAgent()
        );
    }
}
