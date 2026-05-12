package de.tellerstatttonne.backend.distributionpoint.post;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "distribution_post")
public class DistributionPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "distribution_point_id", nullable = false)
    private Long distributionPointId;

    @Column(name = "pickup_run_id", nullable = false, unique = true)
    private Long pickupRunId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "posted_by_id", nullable = false)
    private Long postedById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DistributionPostStatus status = DistributionPostStatus.FRESH;

    @Column(length = 4000)
    private String notes;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("uploadedAt ASC")
    private List<DistributionPostPhotoEntity> photos = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDistributionPointId() { return distributionPointId; }
    public void setDistributionPointId(Long distributionPointId) { this.distributionPointId = distributionPointId; }
    public Long getPickupRunId() { return pickupRunId; }
    public void setPickupRunId(Long pickupRunId) { this.pickupRunId = pickupRunId; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public Long getPostedById() { return postedById; }
    public void setPostedById(Long postedById) { this.postedById = postedById; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public DistributionPostStatus getStatus() { return status; }
    public void setStatus(DistributionPostStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<DistributionPostPhotoEntity> getPhotos() { return photos; }
    public void setPhotos(List<DistributionPostPhotoEntity> photos) { this.photos = photos; }
}
