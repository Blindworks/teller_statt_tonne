package de.tellerstatttonne.backend.onboarding;

public record OnboardingStatusDto(
    boolean hygieneCompleted,
    boolean introductionCompleted,
    boolean profileCompleted,
    boolean agreementCompleted,
    boolean testPickupCompleted,
    boolean allCompleted,
    boolean activated,
    Long introductionBookingId,
    Long introductionSlotId
) {}
