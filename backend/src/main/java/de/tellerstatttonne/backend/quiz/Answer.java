package de.tellerstatttonne.backend.quiz;

public record Answer(
    String id,
    String text,
    Boolean isCorrect,
    Boolean isKnockout
) {
    public static Answer publicView(String id, String text) {
        return new Answer(id, text, null, null);
    }
}
