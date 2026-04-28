package de.tellerstatttonne.backend.partner;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partner")
public class PartnerEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Partner.Category category;

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
    @Column(nullable = false, length = 16)
    private Partner.Status status = Partner.Status.ACTIVE;

    private Double latitude;

    private Double longitude;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Partner.Category getCategory() { return category; }
    public void setCategory(Partner.Category category) { this.category = category; }
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

        public Partner.Weekday getWeekday() { return weekday; }
        public void setWeekday(Partner.Weekday weekday) { this.weekday = weekday; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
