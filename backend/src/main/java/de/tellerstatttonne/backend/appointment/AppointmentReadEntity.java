package de.tellerstatttonne.backend.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "appointment_read",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_appointment_read_user",
        columnNames = {"appointment_id", "user_id"}
    )
)
public class AppointmentReadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    public AppointmentReadEntity() {}

    public AppointmentReadEntity(Long appointmentId, Long userId, Instant readAt) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.readAt = readAt;
    }

    public Long getId() { return id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
