package app.sanctuary.api.messaging;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
public class AnonymousPushController {
    private final PushInstallationRegistry registry;private final PushRegistrationLimiter limiter;
    public AnonymousPushController(PushInstallationRegistry registry,PushRegistrationLimiter limiter){this.registry=registry;this.limiter=limiter;}
    @PutMapping("/push/installations") public Object register(@Valid @RequestBody PushDevicesController.Device device,HttpServletRequest request){
        limiter.check(request.getRemoteAddr());return registry.register(device,null,true);
    }
}
