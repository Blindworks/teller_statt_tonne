package de.tellerstatttonne.backend.user;

import java.util.List;

public record User(
    Long id,
    String email,
    List<String> roles,
    String firstName,
    String lastName,
    String phone,
    String street,
    String postalCode,
    String city,
    String country,
    String photoUrl,
    UserEntity.OnlineStatus onlineStatus,
    UserEntity.Status status,
    List<String> tags
) {
    public User withId(Long newId) {
        return new User(newId, email, roles, firstName, lastName, phone,
            street, postalCode, city, country,
            photoUrl, onlineStatus, status, tags);
    }
}
