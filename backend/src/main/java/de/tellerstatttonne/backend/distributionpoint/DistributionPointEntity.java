package de.tellerstatttonne.backend.distributionpoint;

import de.tellerstatttonne.backend.partner.Partner;
import de.tellerstatttonne.backend.user.UserEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "distribution_point")
public class DistributionPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 255)
    private String street;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(length = 255)
    private String city;

    private Double latitude;

    private Double longitude;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "distribution_point_operator",
        joinColumns = @JoinColumn(name = "distribution_point_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> operators = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "distribution_point_opening_slot",
        joinColumns = @JoinColumn(name = "distribution_point_id")
    )
    @OrderColumn(name = "slot_order")
    private List<OpeningSlotEmbeddable> openingSlots = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Set<UserEntity> getOperators() { return operators; }
    public void setOperators(Set<UserEntity> operators) { this.operators = operators; }
    public List<OpeningSlotEmbeddable> getOpeningSlots() { return openingSlots; }
    public void setOpeningSlots(List<OpeningSlotEmbeddable> openingSlots) { this.openingSlots = openingSlots; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Embeddable
    public static class OpeningSlotEmbeddable {
        @Enumerated(EnumType.STRING)
        @Column(length = 16, nullable = false)
        private Partner.Weekday weekday;

        @Column(name = "start_time", length = 8)
        private String startTime;

        @Column(name = "end_time", length = 8)
        private String endTime;

        public Partner.Weekday getWeekday() { return weekday; }
        public void setWeekday(Partner.Weekday weekday) { this.weekday = weekday; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }
}
