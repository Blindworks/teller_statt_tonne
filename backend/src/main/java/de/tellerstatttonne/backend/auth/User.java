package de.tellerstatttonne.backend.auth;

public record User(
    String id,
    String email,
    Role role,
    String memberId
) {
}
