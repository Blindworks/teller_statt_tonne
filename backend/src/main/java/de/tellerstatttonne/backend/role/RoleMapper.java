package de.tellerstatttonne.backend.role;

public final class RoleMapper {

    private RoleMapper() {}

    public static Role toDto(RoleEntity e, long userCount) {
        return new Role(
            e.getId(),
            e.getName(),
            e.getLabel(),
            e.getDescription(),
            e.getSortOrder(),
            e.isEnabled(),
            userCount
        );
    }
}
