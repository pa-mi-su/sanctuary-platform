package app.sanctuary.api.messaging;

public interface PushGateway {
    boolean enabled();
    record Result(boolean accepted, boolean invalid, String code, String providerId) {}
    Result send(String token, String title, String body, String destination, String messageId);
}
