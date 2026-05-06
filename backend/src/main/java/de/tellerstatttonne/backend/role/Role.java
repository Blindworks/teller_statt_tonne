package de.tellerstatttonne.backend.role;

public record Role(
    Long id,
    String name,
    String label,
    String description,
    Integer sortOrder,
    boolean enabled,
    long userCount
) {
    public static final String ADMIN_ROLE_NAME = "ADMINISTRATOR";
}
