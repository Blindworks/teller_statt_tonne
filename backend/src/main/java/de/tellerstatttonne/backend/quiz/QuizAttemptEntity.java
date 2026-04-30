package de.tellerstatttonne.backend.quiz;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempt")
public class QuizAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "applicant_email", nullable = false)
    private String applicantEmail;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "result_category_label", length = 64)
    private String resultLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_color", nullable = false, length = 16)
    private QuizColor resultColor;

    @Column(name = "knockout_triggered", nullable = false)
    private boolean knockoutTriggered;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "attempt_id", nullable = false)
    @OrderColumn(name = "order_index")
    private List<QuizAttemptAnswerEntity> answers = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getResultLabel() { return resultLabel; }
    public void setResultLabel(String resultLabel) { this.resultLabel = resultLabel; }
    public QuizColor getResultColor() { return resultColor; }
    public void setResultColor(QuizColor resultColor) { this.resultColor = resultColor; }
    public boolean isKnockoutTriggered() { return knockoutTriggered; }
    public void setKnockoutTriggered(boolean knockoutTriggered) { this.knockoutTriggered = knockoutTriggered; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<QuizAttemptAnswerEntity> getAnswers() { return answers; }
    public void setAnswers(List<QuizAttemptAnswerEntity> answers) { this.answers = answers; }
}
