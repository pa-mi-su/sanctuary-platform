package app.sanctuary.api.device.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.device.dto.UserDeviceDto;
import app.sanctuary.api.device.dto.UserDeviceRegistrationRequest;
import app.sanctuary.api.device.service.UserDeviceService;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@RestController
@RequestMapping("/me/devices")
public class UserDeviceController {

    private final UserAccountService userAccountService;
    private final UserDeviceService userDeviceService;

    public UserDeviceController(UserAccountService userAccountService, UserDeviceService userDeviceService) {
        this.userAccountService = userAccountService;
        this.userDeviceService = userDeviceService;
    }

    @GetMapping
    public List<UserDeviceDto> list(Authentication authentication) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return userDeviceService.list(account.id());
    }

    @PutMapping
    public UserDeviceDto register(
        Authentication authentication,
        @Valid @RequestBody UserDeviceRegistrationRequest request
    ) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return userDeviceService.register(account.id(), request);
    }
}
