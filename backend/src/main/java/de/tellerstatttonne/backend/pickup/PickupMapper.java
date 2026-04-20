package de.tellerstatttonne.backend.pickup;

import de.tellerstatttonne.backend.member.Member;
import de.tellerstatttonne.backend.partner.PartnerEntity;
import java.util.List;
import java.util.Map;

final class PickupMapper {

    private PickupMapper() {}

    static Pickup toDto(PickupEntity e, Map<String, Member> membersById) {
        PartnerEntity partner = e.getPartner();
        List<Pickup.Assignment> assignments = e.getAssignments() == null
            ? List.of()
            : e.getAssignments().stream().map(a -> {
                Member m = membersById.get(a.getMemberId());
                if (m == null) {
                    return new Pickup.Assignment(a.getMemberId(), null, null);
                }
                return new Pickup.Assignment(
                    a.getMemberId(),
                    (m.firstName() + " " + m.lastName()).trim(),
                    m.photoUrl()
                );
            }).toList();

        return new Pickup(
            e.getId(),
            partner != null ? partner.getId() : null,
            partner != null ? partner.getName() : null,
            partner != null ? partner.getCategory() : null,
            e.getDate(),
            e.getStartTime(),
            e.getEndTime(),
            e.getStatus(),
            e.getCapacity(),
            assignments,
            e.getNotes()
        );
    }

    static void applyToEntity(PickupEntity target, Pickup src, PartnerEntity partner) {
        target.setPartner(partner);
        target.setDate(src.date());
        target.setStartTime(src.startTime());
        target.setEndTime(src.endTime());
        target.setStatus(src.status() != null ? src.status() : Pickup.Status.SCHEDULED);
        target.setCapacity(src.capacity());
        target.setNotes(src.notes());

        target.getAssignments().clear();
        if (src.assignments() != null) {
            for (Pickup.Assignment a : src.assignments()) {
                PickupEntity.AssignmentEmbeddable emb = new PickupEntity.AssignmentEmbeddable();
                emb.setMemberId(a.memberId());
                target.getAssignments().add(emb);
            }
        }
    }
}
