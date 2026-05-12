package de.tellerstatttonne.backend.pickuprun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pickup_run_item")
public class PickupRunItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_run_id", nullable = false)
    private PickupRunEntity pickupRun;

    @Column(name = "food_category_id")
    private Long foodCategoryId;

    @Column(name = "custom_label", length = 100)
    private String customLabel;

    // Future hook: operator at distribution point can mark item as taken.
    @Column(name = "taken_at")
    private Instant takenAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PickupRunEntity getPickupRun() { return pickupRun; }
    public void setPickupRun(PickupRunEntity pickupRun) { this.pickupRun = pickupRun; }
    public Long getFoodCategoryId() { return foodCategoryId; }
    public void setFoodCategoryId(Long foodCategoryId) { this.foodCategoryId = foodCategoryId; }
    public String getCustomLabel() { return customLabel; }
    public void setCustomLabel(String customLabel) { this.customLabel = customLabel; }
    public Instant getTakenAt() { return takenAt; }
    public void setTakenAt(Instant takenAt) { this.takenAt = takenAt; }
}
