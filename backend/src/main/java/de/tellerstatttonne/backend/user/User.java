package de.tellerstatttonne.backend.user;

import java.util.List;

public record User(
    Long id,
    String email,
    Role role,
    String firstName,
    String lastName,
    String phone,
    String city,
    String photoUrl,
    UserEntity.OnlineStatus onlineStatus,
    UserEntity.Status status,
    List<String> tags
) {
    public User withId(Long newId) {
        return new User(newId, email, role, firstName, lastName, phone, city,
            photoUrl, onlineStatus, status, tags);
    }
}
