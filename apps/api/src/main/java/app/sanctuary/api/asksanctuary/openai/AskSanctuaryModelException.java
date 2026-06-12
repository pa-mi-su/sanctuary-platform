package app.sanctuary.api.asksanctuary.openai;

public class AskSanctuaryModelException extends RuntimeException {
    public AskSanctuaryModelException(String message) {
        super(message);
    }

    public AskSanctuaryModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
