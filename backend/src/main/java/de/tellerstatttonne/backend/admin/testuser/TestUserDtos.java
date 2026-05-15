package de.tellerstatttonne.backend.admin.testuser;

import java.time.Instant;

public final class TestUserDtos {

    private TestUserDtos() {}

    public record CreateTestUserRequest(String firstName, String lastName) {}

    public record TestUserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String status,
        Instant createdAt
    ) {}
}
