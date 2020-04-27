package eu.trentorise.game.challenges.exception;

public class UndefinedChallengeException extends Exception {
    private static final long serialVersionUID = -6050715816393528611L;

    public UndefinedChallengeException(String message, Exception e) {
        super(message, e.getCause());
    }

    public UndefinedChallengeException(String message) {
        super(message);
    }
}