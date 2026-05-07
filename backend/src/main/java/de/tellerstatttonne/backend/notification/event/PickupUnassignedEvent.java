package de.tellerstatttonne.backend.notification.event;

public record PickupUnassignedEvent(Long pickupId, Long actorUserId) {}
