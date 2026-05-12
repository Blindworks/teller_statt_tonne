package de.tellerstatttonne.backend.distributionpoint.post;

import java.time.Instant;
import java.util.List;

public record DistributionPost(
    Long id,
    Long distributionPointId,
    Long pickupRunId,
    Long partnerId,
    Long postedById,
    Instant createdAt,
    Instant updatedAt,
    DistributionPostStatus status,
    String notes,
    List<Photo> photos,
    List<Item> items
) {
    public record Photo(Long id, String imageUrl, Long uploadedById, Instant uploadedAt) {}
    public record Item(Long id, Long foodCategoryId, String customLabel, Instant takenAt) {}
}
