package de.tellerstatttonne.backend.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "quiz_result_category")
public class ResultCategoryEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 64)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuizColor color;

    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;

    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public QuizColor getColor() { return color; }
    public void setColor(QuizColor color) { this.color = color; }
    public BigDecimal getMinScore() { return minScore; }
    public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
