package de.tellerstatttonne.backend.pickuprun;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "pickup_run",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pickup_id", "retter_id"})
)
public class PickupRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pickup_id", nullable = false)
    private Long pickupId;

    @Column(name = "retter_id", nullable = false)
    private Long retterId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PickupRunStatus status = PickupRunStatus.IN_PROGRESS;

    @Column(name = "distribution_point_id")
    private Long distributionPointId;

    @Column(length = 4000)
    private String notes;

    @OneToMany(mappedBy = "pickupRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<PickupRunItemEntity> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPickupId() { return pickupId; }
    public void setPickupId(Long pickupId) { this.pickupId = pickupId; }
    public Long getRetterId() { return retterId; }
    public void setRetterId(Long retterId) { this.retterId = retterId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public PickupRunStatus getStatus() { return status; }
    public void setStatus(PickupRunStatus status) { this.status = status; }
    public Long getDistributionPointId() { return distributionPointId; }
    public void setDistributionPointId(Long distributionPointId) { this.distributionPointId = distributionPointId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<PickupRunItemEntity> getItems() { return items; }
    public void setItems(List<PickupRunItemEntity> items) { this.items = items; }
}
