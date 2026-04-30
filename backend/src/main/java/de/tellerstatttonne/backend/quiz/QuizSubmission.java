package de.tellerstatttonne.backend.quiz;

import java.util.List;

public record QuizSubmission(
    String applicantName,
    String applicantEmail,
    List<SubmittedAnswer> answers
) {
    public record SubmittedAnswer(Long questionId, List<Long> selectedAnswerIds) {}
}
