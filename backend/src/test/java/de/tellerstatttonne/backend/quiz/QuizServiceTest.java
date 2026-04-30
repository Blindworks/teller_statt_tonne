package de.tellerstatttonne.backend.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class QuizServiceTest {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizApplicantLockRepository lockRepository;

    private Question q1;
    private Question q2KO;

    @BeforeEach
    void setup() {
        attemptRepository.deleteAll();
        lockRepository.deleteAll();
        questionRepository.deleteAll();

        q1 = questionService.create(new Question(
            null,
            "Was ist im Umgang mit geretteten Lebensmitteln besonders wichtig?",
            new BigDecimal("1.5"),
            List.of(
                new Answer(null, "Hygiene beachten", true, false),
                new Answer(null, "Keine abgelaufenen Lebensmittel weitergeben", false, false),
                new Answer(null, "Schnelle Verarbeitung", true, false),
                new Answer(null, "Wegwerfen ist okay", false, false)
            )
        ));

        q2KO = questionService.create(new Question(
            null,
            "Wie gehst du mit Abholern um?",
            new BigDecimal("1.0"),
            List.of(
                new Answer(null, "Freundlich", true, false),
                new Answer(null, "Sie haben kein Recht auf Lebensmittel", false, true),
                new Answer(null, "Mit Respekt", true, false),
                new Answer(null, "Egal", false, false)
            )
        ));
    }

    @Test
    void exactCorrectAnswersGivesZeroScoreAndGreen() {
        QuizSubmission submission = new QuizSubmission(
            "Max", "max@example.de",
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), correctIds(q1)),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), correctIds(q2KO))
            )
        );

        QuizResult result = quizService.submit(submission);

        assertThat(result.score()).isEqualByComparingTo("0.0");
        assertThat(result.knockoutTriggered()).isFalse();
        assertThat(result.color()).isEqualTo(QuizColor.GREEN);
    }

    @Test
    void wrongAnswerAddsWeightAsErrorPoints() {
        QuizSubmission submission = new QuizSubmission(
            "Anna", "anna@example.de",
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), List.of(q1.answers().get(0).id())),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), correctIds(q2KO))
            )
        );

        QuizResult result = quizService.submit(submission);

        assertThat(result.score()).isEqualByComparingTo("1.5");
        assertThat(result.color()).isEqualTo(QuizColor.YELLOW);
        assertThat(result.knockoutTriggered()).isFalse();
    }

    @Test
    void tooManyAnswersAlsoCountsAsWrong() {
        List<Long> tooMany = q1.answers().stream().map(Answer::id).toList();

        QuizSubmission submission = new QuizSubmission(
            "Tom", "tom@example.de",
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), tooMany),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), correctIds(q2KO))
            )
        );

        QuizResult result = quizService.submit(submission);

        assertThat(result.score()).isEqualByComparingTo("1.5");
    }

    @Test
    void knockoutAnswerForcesRedRegardlessOfScore() {
        Long knockoutId = q2KO.answers().stream()
            .filter(a -> Boolean.TRUE.equals(a.isKnockout()))
            .findFirst().orElseThrow().id();

        QuizSubmission submission = new QuizSubmission(
            "Lea", "lea@example.de",
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), correctIds(q1)),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), List.of(knockoutId))
            )
        );

        QuizResult result = quizService.submit(submission);

        assertThat(result.knockoutTriggered()).isTrue();
        assertThat(result.color()).isEqualTo(QuizColor.RED);
    }

    @Test
    void allWrongGivesRed() {
        QuizSubmission submission = new QuizSubmission(
            "Joe", "joe@example.de",
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), List.of()),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), List.of())
            )
        );

        QuizResult result = quizService.submit(submission);

        assertThat(result.score()).isEqualByComparingTo("2.5");
        assertThat(result.color()).isEqualTo(QuizColor.RED);
        assertThat(result.knockoutTriggered()).isFalse();
    }

    @Test
    void attemptIsPersistedWithSelectedAnswerSnapshots() {
        QuizSubmission submission = new QuizSubmission(
            "Pia", "pia@example.de",
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), correctIds(q1)),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), correctIds(q2KO))
            )
        );

        QuizResult result = quizService.submit(submission);
        QuizAttempt attempt = quizService.findAttempt(result.attemptId()).orElseThrow();

        assertThat(attempt.applicantName()).isEqualTo("Pia");
        assertThat(attempt.answers()).hasSize(2);
        assertThat(attempt.answers().get(0).selectedAnswers()).isNotEmpty();
        assertThat(attempt.answers().get(0).questionText()).isNotBlank();
    }

    @Test
    void eligibilityLocksAfterMaxAttempts() {
        String email = "limit@example.de";
        // Default max-attempts=3 (siehe application.properties). Drei nicht-bestandene Versuche.
        for (int i = 0; i < 3; i++) {
            quizService.submit(failingSubmission(email));
        }

        Eligibility eligibility = quizService.checkEligibility(email);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).isEqualTo(Eligibility.Reason.LOCKED);
        assertThat(eligibility.attemptsUsed()).isEqualTo(3);
        assertThat(eligibility.attemptsAllowed()).isEqualTo(3);
    }

    @Test
    void submitThrowsWhenLocked() {
        String email = "blocked@example.de";
        for (int i = 0; i < 3; i++) {
            quizService.submit(failingSubmission(email));
        }

        assertThatThrownBy(() -> quizService.submit(failingSubmission(email)))
            .isInstanceOf(QuizNotEligibleException.class)
            .matches(t -> ((QuizNotEligibleException) t).getReason() == Eligibility.Reason.LOCKED);
    }

    @Test
    void passingMakesFurtherAttemptsImpossible() {
        String email = "winner@example.de";
        quizService.submit(passingSubmission(email));

        Eligibility eligibility = quizService.checkEligibility(email);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).isEqualTo(Eligibility.Reason.PASSED);

        assertThatThrownBy(() -> quizService.submit(passingSubmission(email)))
            .isInstanceOf(QuizNotEligibleException.class);
    }

    @Test
    void unlockGrantsFurtherAttempts() {
        String email = "unlock@example.de";
        for (int i = 0; i < 3; i++) {
            quizService.submit(failingSubmission(email));
        }
        assertThat(quizService.checkEligibility(email).eligible()).isFalse();

        quizService.unlock(email);

        Eligibility eligibility = quizService.checkEligibility(email);
        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.attemptsAllowed()).isEqualTo(6);
    }

    @Test
    void emailComparisonIsCaseInsensitive() {
        quizService.submit(failingSubmission("Mixed@Example.de"));
        quizService.submit(failingSubmission("MIXED@example.DE"));
        quizService.submit(failingSubmission("mixed@example.de"));

        Eligibility eligibility = quizService.checkEligibility("MIXED@EXAMPLE.DE");
        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).isEqualTo(Eligibility.Reason.LOCKED);
    }

    @Test
    void findAllApplicantsAggregatesByEmail() {
        quizService.submit(failingSubmission("a@example.de"));
        quizService.submit(failingSubmission("a@example.de"));
        quizService.submit(passingSubmission("b@example.de"));

        List<QuizApplicantStatus> applicants = quizService.findAllApplicants();

        assertThat(applicants).hasSize(2);
        QuizApplicantStatus a = applicants.stream().filter(s -> s.email().equalsIgnoreCase("a@example.de")).findFirst().orElseThrow();
        QuizApplicantStatus b = applicants.stream().filter(s -> s.email().equalsIgnoreCase("b@example.de")).findFirst().orElseThrow();
        assertThat(a.attempts()).isEqualTo(2);
        assertThat(a.locked()).isFalse();
        assertThat(a.passed()).isFalse();
        assertThat(b.passed()).isTrue();
        assertThat(b.locked()).isFalse();
    }

    private QuizSubmission failingSubmission(String email) {
        // Alle Antworten leer → score = 1.5 + 1.0 = 2.5 → RED, kein KO.
        return new QuizSubmission(
            "Test",
            email,
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), List.of()),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), List.of())
            )
        );
    }

    private QuizSubmission passingSubmission(String email) {
        return new QuizSubmission(
            "Test",
            email,
            List.of(
                new QuizSubmission.SubmittedAnswer(q1.id(), correctIds(q1)),
                new QuizSubmission.SubmittedAnswer(q2KO.id(), correctIds(q2KO))
            )
        );
    }

    private static List<Long> correctIds(Question q) {
        return q.answers().stream()
            .filter(a -> Boolean.TRUE.equals(a.isCorrect()))
            .map(Answer::id)
            .toList();
    }
}
