package app.sanctuary.api.auth.service;

public class BlockedEmailDomainException extends RuntimeException {

    public BlockedEmailDomainException(String message) {
        super(message);
    }
}
