package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizAttempt(
    Long id,
    String applicantName,
    String applicantEmail,
    BigDecimal score,
    String resultLabel,
    QuizColor color,
    boolean knockoutTriggered,
    Instant completedAt,
    List<AttemptAnswer> answers
) {
    public record AttemptAnswer(
        Long questionId,
        String questionText,
        BigDecimal questionWeight,
        boolean wasCorrect,
        List<SelectedAnswer> selectedAnswers
    ) {}

    public record SelectedAnswer(
        Long answerId,
        String answerText,
        boolean wasKnockout
    ) {}
}
