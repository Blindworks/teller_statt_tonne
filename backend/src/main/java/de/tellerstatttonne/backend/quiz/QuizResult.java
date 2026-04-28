package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;

public record QuizResult(
    String attemptId,
    BigDecimal score,
    String resultLabel,
    QuizColor color,
    boolean knockoutTriggered
) {}
