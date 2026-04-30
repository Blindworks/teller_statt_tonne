package de.tellerstatttonne.backend.member;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "member")
public class MemberEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private MemberRole role;

    private String email;

    @Column(length = 64)
    private String phone;

    private String city;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false, length = 16)
    private Member.OnlineStatus onlineStatus = Member.OnlineStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Member.Status status = Member.Status.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "member_tag",
        joinColumns = @JoinColumn(name = "member_id")
    )
    @OrderColumn(name = "tag_order")
    @Column(name = "tag", length = 64)
    private List<String> tags = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public MemberRole getRole() { return role; }
    public void setRole(MemberRole role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public Member.OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(Member.OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }
    public Member.Status getStatus() { return status; }
    public void setStatus(Member.Status status) { this.status = status; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
