package de.tellerstatttonne.backend.appointment;

import java.time.Instant;
import java.util.List;

public final class AppointmentDtos {

    private AppointmentDtos() {}

    public record AppointmentInput(
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        String location,
        String attachmentUrl,
        Boolean isPublic,
        List<Long> targetRoleIds
    ) {}

    public record PublicAppointment(
        Long id,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        String location,
        String attachmentUrl
    ) {}

    public record UnreadCount(long count) {}
}
