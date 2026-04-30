package de.tellerstatttonne.backend.member.availability;

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
    name = "member_availability",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_member_availability",
        columnNames = {"member_id", "weekday", "start_time", "end_time"}
    ),
    indexes = @Index(
        name = "idx_member_availability_match",
        columnList = "weekday,start_time,end_time"
    )
)
public class MemberAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", length = 16, nullable = false)
    private Partner.Weekday weekday;

    @Column(name = "start_time", length = 8, nullable = false)
    private String startTime;

    @Column(name = "end_time", length = 8, nullable = false)
    private String endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Partner.Weekday getWeekday() { return weekday; }
    public void setWeekday(Partner.Weekday weekday) { this.weekday = weekday; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
