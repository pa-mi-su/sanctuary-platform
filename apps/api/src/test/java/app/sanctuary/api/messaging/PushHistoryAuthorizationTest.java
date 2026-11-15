package app.sanctuary.api.messaging;
import app.sanctuary.api.admin.AdminAuthService;
import app.sanctuary.api.auth.service.AuthFlowException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PushHistoryAuthorizationTest {
    @Test void clearingHistoryRequiresOwnerAuthenticationBeforeChangingRecords() {
        var auth=mock(AdminAuthService.class); var messages=mock(PushMessages.class);
        when(auth.authenticate(null)).thenThrow(new AuthFlowException(HttpStatus.UNAUTHORIZED,"Sign in required"));
        var controller=new PushMessagesController(auth,messages);
        assertThrows(AuthFlowException.class,()->controller.clearHistory(null,new PushMessages.ClearHistory(UUID.randomUUID())));
        verifyNoInteractions(messages);
    }
}
