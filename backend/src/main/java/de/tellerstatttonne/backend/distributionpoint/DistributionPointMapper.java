package de.tellerstatttonne.backend.distributionpoint;

import de.tellerstatttonne.backend.user.UserEntity;
import java.util.Comparator;
import java.util.List;

final class DistributionPointMapper {

    private DistributionPointMapper() {}

    static DistributionPoint toDto(DistributionPointEntity e) {
        List<DistributionPoint.OperatorRef> operators = e.getOperators() == null
            ? List.of()
            : e.getOperators().stream()
                .sorted(Comparator.comparing(UserEntity::getLastName, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(UserEntity::getFirstName, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(u -> new DistributionPoint.OperatorRef(u.getId(), displayName(u)))
                .toList();

        List<DistributionPoint.OpeningSlot> slots = e.getOpeningSlots() == null
            ? List.of()
            : e.getOpeningSlots().stream()
                .map(s -> new DistributionPoint.OpeningSlot(s.getWeekday(), s.getStartTime(), s.getEndTime()))
                .toList();

        return new DistributionPoint(
            e.getId(),
            e.getName(),
            e.getDescription(),
            e.getStreet(),
            e.getPostalCode(),
            e.getCity(),
            e.getLatitude(),
            e.getLongitude(),
            operators,
            slots
        );
    }

    static void applyScalarFields(DistributionPointEntity target, DistributionPoint src) {
        target.setName(src.name());
        target.setDescription(src.description());
        target.setStreet(src.street());
        target.setPostalCode(src.postalCode());
        target.setCity(src.city());
        target.setLatitude(src.latitude());
        target.setLongitude(src.longitude());

        target.getOpeningSlots().clear();
        if (src.openingSlots() != null) {
            for (DistributionPoint.OpeningSlot slot : src.openingSlots()) {
                DistributionPointEntity.OpeningSlotEmbeddable e = new DistributionPointEntity.OpeningSlotEmbeddable();
                e.setWeekday(slot.weekday());
                e.setStartTime(slot.startTime());
                e.setEndTime(slot.endTime());
                target.getOpeningSlots().add(e);
            }
        }
    }

    private static String displayName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
