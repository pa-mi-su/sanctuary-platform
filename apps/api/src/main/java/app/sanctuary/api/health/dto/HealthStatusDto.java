package app.sanctuary.api.health.dto;

public record HealthStatusDto(
    String status,
    String service,
    String timestamp
) {
}
