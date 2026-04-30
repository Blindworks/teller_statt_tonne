package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;
import java.util.List;

public record Question(
    Long id,
    String text,
    BigDecimal weight,
    List<Answer> answers
) {}
