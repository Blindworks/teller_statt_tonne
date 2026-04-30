package de.tellerstatttonne.backend.quiz;

public class QuizNotEligibleException extends RuntimeException {

    private final Eligibility.Reason reason;

    public QuizNotEligibleException(Eligibility.Reason reason) {
        super("Quiz teilnahme nicht erlaubt: " + reason);
        this.reason = reason;
    }

    public Eligibility.Reason getReason() {
        return reason;
    }
}
