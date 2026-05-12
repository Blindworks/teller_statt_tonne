package de.tellerstatttonne.backend.pickuprun;

import java.time.Instant;
import java.util.List;

public record PickupRun(
    Long id,
    Long pickupId,
    Long retterId,
    Instant startedAt,
    Instant completedAt,
    PickupRunStatus status,
    Long distributionPointId,
    String notes,
    List<PickupRunItem> items
) {
    public record PickupRunItem(
        Long id,
        Long foodCategoryId,
        String customLabel,
        Instant takenAt
    ) {}
}
