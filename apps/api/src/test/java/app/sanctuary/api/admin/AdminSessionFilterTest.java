package app.sanctuary.api.admin;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.sanctuary.api.auth.service.AuthFlowException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminSessionFilterTest {
    @Test void unknownRoutesStillRequireAnAdminSession() throws Exception {
        var auth = mock(AdminAuthService.class);
        when(auth.authenticate(null)).thenThrow(new AuthFlowException(HttpStatus.UNAUTHORIZED, "Sign in again"));
        var request = new MockHttpServletRequest("GET", "/admin-api/future-private-route"); request.addHeader("X-Sanctuary-Admin", "1");
        var response = new MockHttpServletResponse(); var chain = new MockFilterChain();
        new AdminSessionFilter(auth, new ObjectMapper()).doFilter(request, response, chain);
        assertEquals(401, response.getStatus()); assertNull(chain.getRequest());
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
    @Test void onlyExactPostLoginAndVerifyArePublic() throws Exception {
        var auth = mock(AdminAuthService.class);
        var request = new MockHttpServletRequest("POST", "/admin-api/login"); request.addHeader("X-Sanctuary-Admin", "1");
        var chain = new MockFilterChain();
        new AdminSessionFilter(auth, new ObjectMapper()).doFilter(request, new MockHttpServletResponse(), chain);
        verify(auth).requireConfigured(); verify(auth, never()).authenticate(any()); assertNotNull(chain.getRequest());
    }
    @Test void missingCustomHeaderIsDenied() throws Exception {
        var auth = mock(AdminAuthService.class); var response = new MockHttpServletResponse();
        new AdminSessionFilter(auth, new ObjectMapper()).doFilter(new MockHttpServletRequest("POST", "/admin-api/login"), response, new MockFilterChain());
        assertEquals(403, response.getStatus()); verifyNoInteractions(auth);
    }
    @Test void disabledAdminNeverReachesController() throws Exception {
        var auth = mock(AdminAuthService.class); doThrow(new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE, "Disabled")).when(auth).requireConfigured();
        var request = new MockHttpServletRequest("POST", "/admin-api/login"); request.addHeader("X-Sanctuary-Admin", "1");
        var response = new MockHttpServletResponse(); var chain = new MockFilterChain();
        new AdminSessionFilter(auth, new ObjectMapper()).doFilter(request, response, chain);
        assertEquals(503, response.getStatus()); assertNull(chain.getRequest());
    }
}
