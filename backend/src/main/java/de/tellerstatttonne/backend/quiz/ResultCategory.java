package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;

public record ResultCategory(
    String id,
    String label,
    QuizColor color,
    BigDecimal minScore,
    BigDecimal maxScore
) {}
