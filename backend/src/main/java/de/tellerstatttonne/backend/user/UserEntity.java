package de.tellerstatttonne.backend.user;

import de.tellerstatttonne.backend.role.RoleEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "app_user")
public class UserEntity {

    public enum OnlineStatus { ONLINE, AWAY, ON_BREAK, OFFLINE }

    public enum Status { PENDING, ACTIVE, LOCKED, PAUSED, LEFT, REMOVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(length = 64)
    private String phone;

    private String city;

    @Column(length = 255)
    private String street;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false, length = 16)
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "introduction_completed_at")
    private Instant introductionCompletedAt;

    @Column(name = "agreement_file_url", length = 1024)
    private String agreementFileUrl;

    @Column(name = "agreement_uploaded_at")
    private Instant agreementUploadedAt;

    @Column(name = "test_pickup_completed_at")
    private Instant testPickupCompletedAt;

    @Column(name = "is_test_user", nullable = false)
    private boolean testUser = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_tag",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @OrderColumn(name = "tag_order")
    @Column(name = "tag", length = 64)
    private List<String> tags = new ArrayList<>();

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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Set<RoleEntity> getRoles() { return roles; }
    public void setRoles(Set<RoleEntity> roles) { this.roles = roles == null ? new HashSet<>() : roles; }

    public List<String> getRoleNames() {
        return roles.stream()
            .sorted(Comparator.comparing(
                RoleEntity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RoleEntity::getName))
            .map(RoleEntity::getName)
            .collect(Collectors.toList());
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getIntroductionCompletedAt() { return introductionCompletedAt; }
    public void setIntroductionCompletedAt(Instant introductionCompletedAt) { this.introductionCompletedAt = introductionCompletedAt; }
    public String getAgreementFileUrl() { return agreementFileUrl; }
    public void setAgreementFileUrl(String agreementFileUrl) { this.agreementFileUrl = agreementFileUrl; }
    public Instant getAgreementUploadedAt() { return agreementUploadedAt; }
    public void setAgreementUploadedAt(Instant agreementUploadedAt) { this.agreementUploadedAt = agreementUploadedAt; }
    public Instant getTestPickupCompletedAt() { return testPickupCompletedAt; }
    public void setTestPickupCompletedAt(Instant testPickupCompletedAt) { this.testPickupCompletedAt = testPickupCompletedAt; }
    public boolean isTestUser() { return testUser; }
    public void setTestUser(boolean testUser) { this.testUser = testUser; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
