package de.tellerstatttonne.backend.user;

import java.util.ArrayList;
import java.util.List;

public final class UserMapper {

    private UserMapper() {}

    public static User toDto(UserEntity e) {
        return new User(
            e.getId(),
            e.getEmail(),
            e.getRole(),
            e.getFirstName(),
            e.getLastName(),
            e.getPhone(),
            e.getCity(),
            e.getPhotoUrl(),
            e.getOnlineStatus(),
            e.getStatus(),
            e.getTags() == null ? List.of() : List.copyOf(e.getTags())
        );
    }

    public static void applyProfileToEntity(UserEntity target, User src) {
        target.setFirstName(src.firstName());
        target.setLastName(src.lastName());
        target.setRole(src.role());
        if (src.email() != null) {
            target.setEmail(src.email());
        }
        target.setPhone(src.phone());
        target.setCity(src.city());
        target.setPhotoUrl(src.photoUrl());
        target.setOnlineStatus(
            src.onlineStatus() != null ? src.onlineStatus() : UserEntity.OnlineStatus.OFFLINE);
        target.setStatus(src.status() != null ? src.status() : UserEntity.Status.ACTIVE);

        target.getTags().clear();
        if (src.tags() != null) {
            target.getTags().addAll(new ArrayList<>(src.tags()));
        }
    }
}
