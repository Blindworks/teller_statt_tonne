package de.tellerstatttonne.backend.systemlog;

import java.time.Instant;

public record SystemLogFilter(
    SystemLogCategory category,
    SystemLogEventType eventType,
    SystemLogSeverity severity,
    Long actorUserId,
    Instant from,
    Instant to,
    String search
) {}
