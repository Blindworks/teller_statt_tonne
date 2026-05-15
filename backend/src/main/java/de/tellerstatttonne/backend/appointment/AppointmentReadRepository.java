package de.tellerstatttonne.backend.appointment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentReadRepository extends JpaRepository<AppointmentReadEntity, Long> {

    Optional<AppointmentReadEntity> findByAppointmentIdAndUserId(Long appointmentId, Long userId);

    List<AppointmentReadEntity> findByUserIdAndAppointmentIdIn(Long userId, Collection<Long> appointmentIds);

    void deleteByAppointmentId(Long appointmentId);
}
