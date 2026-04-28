package de.tellerstatttonne.backend.quiz;

import static org.assertj.core.api.Assertions.assertThat;

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

    private Question q1;
    private Question q2KO;

    @BeforeEach
    void setup() {
        attemptRepository.deleteAll();
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
        List<String> tooMany = q1.answers().stream().map(Answer::id).toList();

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
        String knockoutId = q2KO.answers().stream()
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

    private static List<String> correctIds(Question q) {
        return q.answers().stream()
            .filter(a -> Boolean.TRUE.equals(a.isCorrect()))
            .map(Answer::id)
            .toList();
    }
}
