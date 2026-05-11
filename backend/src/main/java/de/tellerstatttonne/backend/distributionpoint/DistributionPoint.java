package de.tellerstatttonne.backend.distributionpoint;

import de.tellerstatttonne.backend.partner.Partner;
import java.util.List;

public record DistributionPoint(
    Long id,
    String name,
    String description,
    String street,
    String postalCode,
    String city,
    Double latitude,
    Double longitude,
    List<OperatorRef> operators,
    List<OpeningSlot> openingSlots
) {
    public record OperatorRef(Long id, String displayName) {}

    public record OpeningSlot(Partner.Weekday weekday, String startTime, String endTime) {}
}
