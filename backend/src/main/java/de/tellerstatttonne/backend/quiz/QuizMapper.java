package de.tellerstatttonne.backend.quiz;

import java.util.List;

final class QuizMapper {

    private QuizMapper() {}

    static Question toDto(QuestionEntity e, boolean includeSolutions) {
        List<Answer> answers = e.getAnswers().stream()
            .map(a -> includeSolutions
                ? new Answer(a.getId(), a.getText(), a.isCorrect(), a.isKnockout())
                : Answer.publicView(a.getId(), a.getText()))
            .toList();
        return new Question(e.getId(), e.getText(), e.getWeight(), answers);
    }

    static void applyToEntity(QuestionEntity target, Question src) {
        target.setText(src.text());
        target.setWeight(src.weight());

        target.getAnswers().clear();
        if (src.answers() != null) {
            for (Answer a : src.answers()) {
                AnswerEntity entity = new AnswerEntity();
                if (a.id() != null) {
                    entity.setId(a.id());
                }
                entity.setText(a.text());
                entity.setCorrect(Boolean.TRUE.equals(a.isCorrect()));
                entity.setKnockout(Boolean.TRUE.equals(a.isKnockout()));
                target.getAnswers().add(entity);
            }
        }
    }

    static ResultCategory toDto(ResultCategoryEntity e) {
        return new ResultCategory(e.getId(), e.getLabel(), e.getColor(), e.getMinScore(), e.getMaxScore());
    }

    static void applyToEntity(ResultCategoryEntity target, ResultCategory src) {
        target.setLabel(src.label());
        target.setColor(src.color());
        target.setMinScore(src.minScore());
        target.setMaxScore(src.maxScore());
    }

    static QuizAttempt toDto(QuizAttemptEntity e) {
        List<QuizAttempt.AttemptAnswer> answers = e.getAnswers().stream()
            .map(a -> new QuizAttempt.AttemptAnswer(
                a.getQuestionId(),
                a.getQuestionText(),
                a.getQuestionWeight(),
                a.isWasCorrect(),
                a.getSelectedAnswers().stream()
                    .map(s -> new QuizAttempt.SelectedAnswer(s.getAnswerId(), s.getAnswerText(), s.isWasKnockout()))
                    .toList()
            ))
            .toList();
        return new QuizAttempt(
            e.getId(),
            e.getApplicantName(),
            e.getApplicantEmail(),
            e.getScore(),
            e.getResultLabel(),
            e.getResultColor(),
            e.isKnockoutTriggered(),
            e.getCompletedAt(),
            answers
        );
    }
}
