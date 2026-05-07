package de.tellerstatttonne.backend.notification.event;

import de.tellerstatttonne.backend.pickup.Pickup;

public record PickupStatusChangedEvent(
    Long pickupId,
    Pickup.Status oldStatus,
    Pickup.Status newStatus,
    Long actorUserId
) {}
