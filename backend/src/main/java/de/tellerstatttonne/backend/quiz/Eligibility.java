package de.tellerstatttonne.backend.quiz;

public record Eligibility(
    boolean eligible,
    Reason reason,
    long attemptsUsed,
    int attemptsAllowed
) {
    public enum Reason {
        PASSED,
        LOCKED
    }

    public static Eligibility ok(long attemptsUsed, int attemptsAllowed) {
        return new Eligibility(true, null, attemptsUsed, attemptsAllowed);
    }

    public static Eligibility blocked(Reason reason, long attemptsUsed, int attemptsAllowed) {
        return new Eligibility(false, reason, attemptsUsed, attemptsAllowed);
    }
}
