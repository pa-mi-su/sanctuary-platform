package app.sanctuary.api.messaging;
import app.sanctuary.api.admin.AdminAuthService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(value="/admin-api/messages",headers="X-Sanctuary-Admin=1")
public class PushMessagesController {
    private final AdminAuthService auth; private final PushMessages messages;
    public PushMessagesController(AdminAuthService auth,PushMessages messages){this.auth=auth;this.messages=messages;}
    @GetMapping public Object list(@RequestHeader(value="Authorization",required=false)String token,@RequestParam(defaultValue="0")int page){auth.authenticate(token);return messages.overview(page);}
    @PostMapping("/preview")public Object preview(@RequestHeader(value="Authorization",required=false)String token,@RequestBody PushMessages.Draft draft){auth.authenticate(token);return messages.preview(draft);}
    @PostMapping public Object send(@RequestHeader(value="Authorization",required=false)String token,@RequestBody PushMessages.Send request){return messages.send(auth.authenticate(token),request);}
}
