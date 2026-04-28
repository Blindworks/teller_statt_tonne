package de.tellerstatttonne.backend.quiz;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempt_answer")
public class QuizAttemptAnswerEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "question_id", length = 36)
    private String questionId;

    @Column(name = "question_text", length = 2048)
    private String questionText;

    @Column(name = "question_weight", nullable = false, precision = 3, scale = 1)
    private BigDecimal questionWeight;

    @Column(name = "was_correct", nullable = false)
    private boolean wasCorrect;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "quiz_attempt_selected_answer",
        joinColumns = @JoinColumn(name = "attempt_answer_id")
    )
    @OrderColumn(name = "selection_order")
    private List<SelectedAnswerEmbeddable> selectedAnswers = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public BigDecimal getQuestionWeight() { return questionWeight; }
    public void setQuestionWeight(BigDecimal questionWeight) { this.questionWeight = questionWeight; }
    public boolean isWasCorrect() { return wasCorrect; }
    public void setWasCorrect(boolean wasCorrect) { this.wasCorrect = wasCorrect; }
    public List<SelectedAnswerEmbeddable> getSelectedAnswers() { return selectedAnswers; }
    public void setSelectedAnswers(List<SelectedAnswerEmbeddable> selectedAnswers) { this.selectedAnswers = selectedAnswers; }

    @Embeddable
    public static class SelectedAnswerEmbeddable {
        @Column(name = "answer_id", length = 36)
        private String answerId;

        @Column(name = "answer_text", length = 1024)
        private String answerText;

        @Column(name = "was_knockout", nullable = false)
        private boolean wasKnockout;

        public String getAnswerId() { return answerId; }
        public void setAnswerId(String answerId) { this.answerId = answerId; }
        public String getAnswerText() { return answerText; }
        public void setAnswerText(String answerText) { this.answerText = answerText; }
        public boolean isWasKnockout() { return wasKnockout; }
        public void setWasKnockout(boolean wasKnockout) { this.wasKnockout = wasKnockout; }
    }
}
