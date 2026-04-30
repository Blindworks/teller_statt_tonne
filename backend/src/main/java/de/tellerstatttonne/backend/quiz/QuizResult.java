package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;

public record QuizResult(
    Long attemptId,
    BigDecimal score,
    String resultLabel,
    QuizColor color,
    boolean knockoutTriggered
) {}
