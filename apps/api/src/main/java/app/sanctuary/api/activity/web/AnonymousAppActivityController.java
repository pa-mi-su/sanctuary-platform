package app.sanctuary.api.activity.web;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.activity.dto.AnonymousAppActivityRequest;
import app.sanctuary.api.activity.service.UserAppActivityService;

@RestController
@RequestMapping("/app/activity")
public class AnonymousAppActivityController {

    private final UserAppActivityService activityService;

    public AnonymousAppActivityController(UserAppActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<Void> record(@Valid @RequestBody AnonymousAppActivityRequest request) {
        activityService.recordAnonymous(request);
        return ResponseEntity.noContent().build();
    }
}
