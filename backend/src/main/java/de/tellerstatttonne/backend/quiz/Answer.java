package de.tellerstatttonne.backend.quiz;

public record Answer(
    Long id,
    String text,
    Boolean isCorrect,
    Boolean isKnockout
) {
    public static Answer publicView(Long id, String text) {
        return new Answer(id, text, null, null);
    }
}
