package de.tellerstatttonne.backend.appointment;

import de.tellerstatttonne.backend.role.Role;
import java.time.Instant;
import java.util.List;

public record Appointment(
    Long id,
    String title,
    String description,
    Instant startTime,
    Instant endTime,
    String location,
    String attachmentUrl,
    boolean isPublic,
    Long createdById,
    List<Role> targetRoles,
    boolean read,
    boolean canEdit,
    Instant createdAt,
    Instant updatedAt
) {}
