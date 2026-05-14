package de.tellerstatttonne.backend.introduction;

import java.time.LocalDate;
import java.time.LocalTime;

public record IntroductionSlotRequest(
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String location,
    Integer capacity,
    String notes
) {}
