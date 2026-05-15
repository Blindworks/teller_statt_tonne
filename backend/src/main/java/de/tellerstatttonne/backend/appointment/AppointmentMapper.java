package de.tellerstatttonne.backend.appointment;

import de.tellerstatttonne.backend.role.Role;
import de.tellerstatttonne.backend.role.RoleEntity;
import de.tellerstatttonne.backend.role.RoleMapper;
import java.util.Comparator;
import java.util.List;

final class AppointmentMapper {

    private AppointmentMapper() {}

    static Appointment toDto(AppointmentEntity e, boolean read, boolean canEdit) {
        List<Role> roles = e.getTargetRoles().stream()
            .sorted(Comparator.comparing(
                RoleEntity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RoleEntity::getName))
            .map(r -> RoleMapper.toDto(r, 0L))
            .toList();
        return new Appointment(
            e.getId(),
            e.getTitle(),
            e.getDescription(),
            e.getStartTime(),
            e.getEndTime(),
            e.getLocation(),
            e.getAttachmentUrl(),
            e.isPublic(),
            e.getCreatedById(),
            roles,
            read,
            canEdit,
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    static AppointmentDtos.PublicAppointment toPublicDto(AppointmentEntity e) {
        return new AppointmentDtos.PublicAppointment(
            e.getId(),
            e.getTitle(),
            e.getDescription(),
            e.getStartTime(),
            e.getEndTime(),
            e.getLocation(),
            e.getAttachmentUrl()
        );
    }
}
