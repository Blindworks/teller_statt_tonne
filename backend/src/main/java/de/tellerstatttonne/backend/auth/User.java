package de.tellerstatttonne.backend.auth;

public record User(
    Long id,
    String email,
    Role role,
    Long memberId
) {
}
