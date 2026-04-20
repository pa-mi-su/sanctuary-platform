package app.sanctuary.api.health.web;

import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.health.dto.HealthStatusDto;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public HealthStatusDto health() {
        return new HealthStatusDto("ok", "sanctuary-api", OffsetDateTime.now().toString());
    }
}
