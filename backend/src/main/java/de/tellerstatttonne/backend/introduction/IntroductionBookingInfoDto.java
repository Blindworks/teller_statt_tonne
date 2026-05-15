package de.tellerstatttonne.backend.introduction;

public record IntroductionBookingInfoDto(
    Long bookingId,
    Long userId,
    String firstName,
    String lastName,
    String email
) {}
