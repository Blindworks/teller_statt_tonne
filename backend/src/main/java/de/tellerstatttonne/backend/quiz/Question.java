package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;
import java.util.List;

public record Question(
    String id,
    String text,
    BigDecimal weight,
    List<Answer> answers
) {}
