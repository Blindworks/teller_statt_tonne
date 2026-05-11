package de.tellerstatttonne.backend.partner;

import de.tellerstatttonne.backend.user.UserEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "partner")
public class PartnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    private String street;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    private String city;

    @Column(name = "logo_url", length = 1024)
    private String logoUrl;

    @Embedded
    private ContactEmbeddable contact = new ContactEmbeddable();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "partner_pickup_slot",
        joinColumns = @JoinColumn(name = "partner_id")
    )
    @OrderColumn(name = "slot_order")
    private List<PickupSlotEmbeddable> pickupSlots = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Partner.Status status = Partner.Status.KEIN_KONTAKT;

    private Double latitude;

    private Double longitude;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "partner_user",
        joinColumns = @JoinColumn(name = "partner_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> members = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public ContactEmbeddable getContact() { return contact; }
    public void setContact(ContactEmbeddable contact) { this.contact = contact; }
    public List<PickupSlotEmbeddable> getPickupSlots() { return pickupSlots; }
    public void setPickupSlots(List<PickupSlotEmbeddable> pickupSlots) { this.pickupSlots = pickupSlots; }
    public Partner.Status getStatus() { return status; }
    public void setStatus(Partner.Status status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Set<UserEntity> getMembers() { return members; }
    public void setMembers(Set<UserEntity> members) { this.members = members; }

    @Embeddable
    public static class ContactEmbeddable {
        @Column(name = "contact_name")
        private String name;
        @Column(name = "contact_email")
        private String email;
        @Column(name = "contact_phone", length = 64)
        private String phone;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    @Embeddable
    public static class PickupSlotEmbeddable {
        @Enumerated(EnumType.STRING)
        @Column(length = 16, nullable = false)
        private Partner.Weekday weekday;
        @Column(name = "start_time", length = 8)
        private String startTime;
        @Column(name = "end_time", length = 8)
        private String endTime;
        @Column(nullable = false)
        private boolean active;
        @Column(nullable = false)
        private int capacity = 1;
        @Column(name = "expected_kg", precision = 10, scale = 2)
        private java.math.BigDecimal expectedKg;

        public Partner.Weekday getWeekday() { return weekday; }
        public void setWeekday(Partner.Weekday weekday) { this.weekday = weekday; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public java.math.BigDecimal getExpectedKg() { return expectedKg; }
        public void setExpectedKg(java.math.BigDecimal expectedKg) { this.expectedKg = expectedKg; }
    }
}
