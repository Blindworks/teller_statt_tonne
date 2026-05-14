package de.tellerstatttonne.backend.introduction;

import java.time.LocalDate;
import java.time.LocalTime;

public record IntroductionSlotDto(
    Long id,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String location,
    int capacity,
    int bookedCount,
    String notes,
    boolean bookedByMe
) {}
