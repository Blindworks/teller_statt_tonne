package de.tellerstatttonne.backend.quiz;

import java.time.Instant;

public record QuizApplicantStatus(
    String email,
    String name,
    long attempts,
    int attemptsAllowed,
    boolean locked,
    boolean passed,
    Instant lastAttemptAt,
    QuizColor lastResultColor,
    boolean userExists
) {}
