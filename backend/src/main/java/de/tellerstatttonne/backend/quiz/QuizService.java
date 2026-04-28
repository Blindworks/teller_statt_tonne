package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuizService {

    private final QuestionRepository questionRepository;
    private final ResultCategoryRepository categoryRepository;
    private final QuizAttemptRepository attemptRepository;

    public QuizService(
        QuestionRepository questionRepository,
        ResultCategoryRepository categoryRepository,
        QuizAttemptRepository attemptRepository
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.attemptRepository = attemptRepository;
    }

    public QuizResult submit(QuizSubmission submission) {
        validateSubmission(submission);

        List<QuestionEntity> questions = questionRepository.findAllByOrderByOrderIndexAsc();
        Map<String, QuestionEntity> questionsById = questions.stream()
            .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

        Map<String, QuizSubmission.SubmittedAnswer> submittedById = submission.answers() == null
            ? Map.of()
            : submission.answers().stream()
                .filter(a -> a.questionId() != null)
                .collect(Collectors.toMap(
                    QuizSubmission.SubmittedAnswer::questionId,
                    a -> a,
                    (a, b) -> a));

        BigDecimal score = BigDecimal.ZERO;
        boolean knockout = false;

        QuizAttemptEntity attempt = new QuizAttemptEntity();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setApplicantName(submission.applicantName().trim());
        attempt.setApplicantEmail(submission.applicantEmail().trim());
        attempt.setCompletedAt(Instant.now());

        for (QuestionEntity question : questions) {
            QuizSubmission.SubmittedAnswer submitted = submittedById.get(question.getId());
            Set<String> selectedIds = submitted == null || submitted.selectedAnswerIds() == null
                ? Set.of()
                : new HashSet<>(submitted.selectedAnswerIds());

            Map<String, AnswerEntity> answersById = question.getAnswers().stream()
                .collect(Collectors.toMap(AnswerEntity::getId, a -> a));

            Set<String> correctIds = question.getAnswers().stream()
                .filter(AnswerEntity::isCorrect)
                .map(AnswerEntity::getId)
                .collect(Collectors.toSet());

            QuizAttemptAnswerEntity attemptAnswer = new QuizAttemptAnswerEntity();
            attemptAnswer.setId(UUID.randomUUID().toString());
            attemptAnswer.setQuestionId(question.getId());
            attemptAnswer.setQuestionText(question.getText());
            attemptAnswer.setQuestionWeight(question.getWeight());

            for (String selectedId : selectedIds) {
                AnswerEntity ans = answersById.get(selectedId);
                QuizAttemptAnswerEntity.SelectedAnswerEmbeddable sel =
                    new QuizAttemptAnswerEntity.SelectedAnswerEmbeddable();
                sel.setAnswerId(selectedId);
                sel.setAnswerText(ans != null ? ans.getText() : null);
                sel.setWasKnockout(ans != null && ans.isKnockout());
                attemptAnswer.getSelectedAnswers().add(sel);
                if (ans != null && ans.isKnockout()) {
                    knockout = true;
                }
            }

            boolean correct = selectedIds.equals(correctIds);
            attemptAnswer.setWasCorrect(correct);
            if (!correct) {
                score = score.add(question.getWeight());
            }

            attempt.getAnswers().add(attemptAnswer);
        }

        ResultCategoryEntity category = determineCategory(score, knockout);
        attempt.setScore(score);
        attempt.setKnockoutTriggered(knockout);
        if (category != null) {
            attempt.setResultLabel(category.getLabel());
            attempt.setResultColor(category.getColor());
        } else {
            attempt.setResultLabel(null);
            attempt.setResultColor(knockout ? QuizColor.RED : QuizColor.GREEN);
        }

        QuizAttemptEntity saved = attemptRepository.save(attempt);
        return new QuizResult(
            saved.getId(),
            saved.getScore(),
            saved.getResultLabel(),
            saved.getResultColor(),
            saved.isKnockoutTriggered()
        );
    }

    private ResultCategoryEntity determineCategory(BigDecimal score, boolean knockout) {
        List<ResultCategoryEntity> categories = categoryRepository.findAllByOrderByOrderIndexAsc();
        if (knockout) {
            return categories.stream()
                .filter(c -> c.getColor() == QuizColor.RED)
                .findFirst()
                .orElse(null);
        }
        return categories.stream()
            .filter(c -> score.compareTo(c.getMinScore()) >= 0
                && (c.getMaxScore() == null || score.compareTo(c.getMaxScore()) <= 0))
            .findFirst()
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<QuizAttempt> findAllAttempts() {
        return attemptRepository.findAllByOrderByCompletedAtDesc().stream()
            .map(QuizMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<QuizAttempt> findAttempt(String id) {
        return attemptRepository.findById(id).map(QuizMapper::toDto);
    }

    private void validateSubmission(QuizSubmission s) {
        if (s == null) {
            throw new IllegalArgumentException("submission is required");
        }
        if (s.applicantName() == null || s.applicantName().isBlank()) {
            throw new IllegalArgumentException("applicantName is required");
        }
        if (s.applicantEmail() == null || s.applicantEmail().isBlank()) {
            throw new IllegalArgumentException("applicantEmail is required");
        }
    }
}
