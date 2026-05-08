package de.tellerstatttonne.backend.user;

import java.util.ArrayList;
import java.util.List;

public final class UserMapper {

    private UserMapper() {}

    public static User toDto(UserEntity e) {
        return toDto(e, false);
    }

    public static User toDto(UserEntity e, boolean hygieneApproved) {
        return new User(
            e.getId(),
            e.getEmail(),
            e.getRoleNames(),
            e.getFirstName(),
            e.getLastName(),
            e.getPhone(),
            e.getStreet(),
            e.getPostalCode(),
            e.getCity(),
            e.getCountry(),
            e.getPhotoUrl(),
            e.getOnlineStatus(),
            e.getStatus(),
            e.getIntroductionCompletedAt(),
            hygieneApproved,
            e.getPasswordHash() != null,
            e.getTags() == null ? List.of() : List.copyOf(e.getTags())
        );
    }

    /**
     * Updates non-role profile fields. Role assignment is handled separately
     * by {@code UserService} so it can resolve role names to entities.
     * Status changes go through the dedicated transition methods on
     * {@code UserService} and are intentionally not applied here.
     */
    public static void applyProfileToEntity(UserEntity target, User src) {
        target.setFirstName(src.firstName());
        target.setLastName(src.lastName());
        if (src.email() != null) {
            target.setEmail(src.email());
        }
        target.setPhone(src.phone());
        target.setStreet(src.street());
        target.setPostalCode(src.postalCode());
        target.setCity(src.city());
        target.setCountry(src.country());
        target.setPhotoUrl(src.photoUrl());
        target.setOnlineStatus(
            src.onlineStatus() != null ? src.onlineStatus() : UserEntity.OnlineStatus.OFFLINE);

        target.getTags().clear();
        if (src.tags() != null) {
            target.getTags().addAll(new ArrayList<>(src.tags()));
        }
    }
}
