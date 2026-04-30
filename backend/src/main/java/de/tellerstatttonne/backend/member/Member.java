package de.tellerstatttonne.backend.member;

import java.util.List;

public record Member(
    String id,
    String firstName,
    String lastName,
    MemberRole role,
    String email,
    String phone,
    String city,
    String photoUrl,
    OnlineStatus onlineStatus,
    Status status,
    List<String> tags
) {
    public enum OnlineStatus { ONLINE, AWAY, ON_BREAK, OFFLINE }

    public enum Status { ACTIVE, PENDING, INACTIVE }

    public Member withId(String newId) {
        return new Member(newId, firstName, lastName, role, email, phone, city,
            photoUrl, onlineStatus, status, tags);
    }
}
