package de.tellerstatttonne.backend.systemlog.event;

import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.SystemLogSeverity;

public record SystemLogEvent(
    SystemLogEventType eventType,
    SystemLogSeverity severity,
    Long actorUserId,
    String actorEmail,
    String targetType,
    Long targetId,
    String message,
    String details
) {

    public static Builder of(SystemLogEventType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final SystemLogEventType eventType;
        private SystemLogSeverity severity;
        private Long actorUserId;
        private String actorEmail;
        private String targetType;
        private Long targetId;
        private String message;
        private String details;

        private Builder(SystemLogEventType eventType) {
            this.eventType = eventType;
            this.severity = eventType.getDefaultSeverity();
        }

        public Builder severity(SystemLogSeverity severity) { this.severity = severity; return this; }
        public Builder actor(Long userId, String email) { this.actorUserId = userId; this.actorEmail = email; return this; }
        public Builder actorUserId(Long userId) { this.actorUserId = userId; return this; }
        public Builder actorEmail(String email) { this.actorEmail = email; return this; }
        public Builder target(String type, Long id) { this.targetType = type; this.targetId = id; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder details(String details) { this.details = details; return this; }

        public SystemLogEvent build() {
            return new SystemLogEvent(
                eventType, severity, actorUserId, actorEmail,
                targetType, targetId, message, details
            );
        }
    }
}
