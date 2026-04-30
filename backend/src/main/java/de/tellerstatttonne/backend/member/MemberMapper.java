package de.tellerstatttonne.backend.member;

import java.util.ArrayList;
import java.util.List;

final class MemberMapper {

    private MemberMapper() {}

    static Member toDto(MemberEntity e) {
        return new Member(
            e.getId(),
            e.getFirstName(),
            e.getLastName(),
            e.getRole(),
            e.getEmail(),
            e.getPhone(),
            e.getCity(),
            e.getPhotoUrl(),
            e.getOnlineStatus(),
            e.getStatus(),
            e.getTags() == null ? List.of() : List.copyOf(e.getTags())
        );
    }

    static void applyToEntity(MemberEntity target, Member src) {
        target.setFirstName(src.firstName());
        target.setLastName(src.lastName());
        target.setRole(src.role());
        target.setEmail(src.email());
        target.setPhone(src.phone());
        target.setCity(src.city());
        target.setPhotoUrl(src.photoUrl());
        target.setOnlineStatus(
            src.onlineStatus() != null ? src.onlineStatus() : Member.OnlineStatus.OFFLINE);
        target.setStatus(src.status() != null ? src.status() : Member.Status.ACTIVE);

        target.getTags().clear();
        if (src.tags() != null) {
            target.getTags().addAll(new ArrayList<>(src.tags()));
        }
    }
}
