package de.tellerstatttonne.backend.quiz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import de.tellerstatttonne.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuizService {

    private final QuestionRepository questionRepository;
    private final ResultCategoryRepository categoryRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizApplicantLockRepository lockRepository;
    private final UserRepository userRepository;
    private final int maxAttempts;

    public QuizService(
        QuestionRepository questionRepository,
        ResultCategoryRepository categoryRepository,
        QuizAttemptRepository attemptRepository,
        QuizApplicantLockRepository lockRepository,
        UserRepository userRepository,
        @Value("${quiz.max-attempts:3}") int maxAttempts
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.attemptRepository = attemptRepository;
        this.lockRepository = lockRepository;
        this.userRepository = userRepository;
        this.maxAttempts = maxAttempts;
    }

    public QuizResult submit(QuizSubmission submission) {
        validateSubmission(submission);
        Eligibility eligibility = checkEligibility(submission.applicantEmail());
        if (!eligibility.eligible()) {
            throw new QuizNotEligibleException(eligibility.reason());
        }

        List<QuestionEntity> questions = questionRepository.findAllByOrderByOrderIndexAsc();
        Map<Long, QuestionEntity> questionsById = questions.stream()
            .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

        Map<Long, QuizSubmission.SubmittedAnswer> submittedById = submission.answers() == null
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
        attempt.setApplicantName(submission.applicantName().trim());
        attempt.setApplicantEmail(submission.applicantEmail().trim());
        attempt.setCompletedAt(Instant.now());

        for (QuestionEntity question : questions) {
            QuizSubmission.SubmittedAnswer submitted = submittedById.get(question.getId());
            Set<Long> selectedIds = submitted == null || submitted.selectedAnswerIds() == null
                ? Set.of()
                : new HashSet<>(submitted.selectedAnswerIds());

            Map<Long, AnswerEntity> answersById = question.getAnswers().stream()
                .collect(Collectors.toMap(AnswerEntity::getId, a -> a));

            Set<Long> correctIds = question.getAnswers().stream()
                .filter(AnswerEntity::isCorrect)
                .map(AnswerEntity::getId)
                .collect(Collectors.toSet());

            QuizAttemptAnswerEntity attemptAnswer = new QuizAttemptAnswerEntity();
            attemptAnswer.setQuestionId(question.getId());
            attemptAnswer.setQuestionText(question.getText());
            attemptAnswer.setQuestionWeight(question.getWeight());

            for (Long selectedId : selectedIds) {
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
    public java.util.Optional<QuizAttempt> findAttempt(Long id) {
        return attemptRepository.findById(id).map(QuizMapper::toDto);
    }

    public boolean deleteAttempt(Long id) {
        if (!attemptRepository.existsById(id)) {
            return false;
        }
        attemptRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public Eligibility checkEligibility(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        String email = rawEmail.trim();
        int bonus = lockRepository.findByEmail(email.toLowerCase())
            .map(QuizApplicantLockEntity::getAttemptsBonus)
            .orElse(0);
        int allowed = maxAttempts + bonus;
        long used = attemptRepository.countByApplicantEmailIgnoreCase(email);
        boolean passed = attemptRepository.existsByApplicantEmailIgnoreCaseAndResultColor(email, QuizColor.GREEN);
        if (passed) {
            return Eligibility.blocked(Eligibility.Reason.PASSED, used, allowed);
        }
        if (used >= allowed) {
            return Eligibility.blocked(Eligibility.Reason.LOCKED, used, allowed);
        }
        return Eligibility.ok(used, allowed);
    }

    public void unlock(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        String email = rawEmail.trim().toLowerCase();
        QuizApplicantLockEntity lock = lockRepository.findByEmail(email)
            .orElseGet(() -> {
                QuizApplicantLockEntity created = new QuizApplicantLockEntity();
                created.setEmail(email);
                created.setAttemptsBonus(0);
                return created;
            });
        lock.setAttemptsBonus(lock.getAttemptsBonus() + maxAttempts);
        lock.setUpdatedAt(Instant.now());
        lockRepository.save(lock);
    }

    @Transactional(readOnly = true)
    public List<QuizApplicantStatus> findAllApplicants() {
        List<QuizAttemptEntity> attempts = attemptRepository.findAllByOrderByCompletedAtDesc();
        Map<String, List<QuizAttemptEntity>> byEmail = new HashMap<>();
        for (QuizAttemptEntity a : attempts) {
            String key = a.getApplicantEmail() == null ? "" : a.getApplicantEmail().trim().toLowerCase();
            byEmail.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        Map<String, Integer> bonusByEmail = lockRepository.findAll().stream()
            .collect(Collectors.toMap(
                l -> l.getEmail().toLowerCase(),
                QuizApplicantLockEntity::getAttemptsBonus,
                (a, b) -> a));

        List<QuizApplicantStatus> result = new ArrayList<>();
        for (Map.Entry<String, List<QuizAttemptEntity>> entry : byEmail.entrySet()) {
            List<QuizAttemptEntity> list = entry.getValue();
            QuizAttemptEntity latest = list.get(0);
            long count = list.size();
            boolean passed = list.stream().anyMatch(a -> a.getResultColor() == QuizColor.GREEN);
            int allowed = maxAttempts + bonusByEmail.getOrDefault(entry.getKey(), 0);
            boolean locked = !passed && count >= allowed;
            boolean userExists = entry.getKey().isEmpty() ? false : userRepository.existsByEmail(entry.getKey());
            result.add(new QuizApplicantStatus(
                latest.getApplicantEmail(),
                latest.getApplicantName(),
                count,
                allowed,
                locked,
                passed,
                latest.getCompletedAt(),
                latest.getResultColor(),
                userExists
            ));
        }
        result.sort(Comparator.comparing(QuizApplicantStatus::lastAttemptAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
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
