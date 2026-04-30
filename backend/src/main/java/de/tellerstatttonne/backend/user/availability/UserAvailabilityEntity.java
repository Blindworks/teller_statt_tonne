package de.tellerstatttonne.backend.user.availability;

import de.tellerstatttonne.backend.partner.Partner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "user_availability",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_availability",
        columnNames = {"user_id", "weekday", "start_time", "end_time"}
    ),
    indexes = @Index(
        name = "idx_user_availability_match",
        columnList = "weekday,start_time,end_time"
    )
)
public class UserAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", length = 16, nullable = false)
    private Partner.Weekday weekday;

    @Column(name = "start_time", length = 8, nullable = false)
    private String startTime;

    @Column(name = "end_time", length = 8, nullable = false)
    private String endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Partner.Weekday getWeekday() { return weekday; }
    public void setWeekday(Partner.Weekday weekday) { this.weekday = weekday; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
