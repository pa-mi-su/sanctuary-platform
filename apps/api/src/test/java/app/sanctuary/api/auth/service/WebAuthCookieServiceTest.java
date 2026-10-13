package app.sanctuary.api.auth.service;

import app.sanctuary.api.auth.dto.AuthSessionResponse;
import app.sanctuary.api.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class WebAuthCookieServiceTest {
    @Test void logoutClearsBothMeAndAdminCookies(){
        var service=new WebAuthCookieService(new AuthProperties());
        var response=new MockHttpServletResponse();service.clearSessionCookies(response);
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(c->c.contains("Path=/admin")&&c.contains("Max-Age=0")&&c.contains("HttpOnly"));
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(c->c.contains("Path=/me")&&c.contains("Max-Age=0"));
    }
    @Test void ordinarySignInNeverCreatesAdministratorCookies(){
        var service=new WebAuthCookieService(new AuthProperties());var response=new MockHttpServletResponse();
        service.setSessionCookies(response,new AuthSessionResponse("access","id","refresh","Bearer",3600,"user@example.org","User"));
        assertThat(response.getHeaders("Set-Cookie")).noneMatch(c->c.contains("Path=/admin")||c.contains("sanctuary_news_mfa"));
    }

    @Test void administratorCookiesAreSecureHttpOnlyAndHostOnlyEvenWithConsumerDomain() {
        var auth=new AuthProperties();auth.cookie().setDomain(".mydailysanctuary.com");
        var response=new MockHttpServletResponse();
        new WebAuthCookieService(auth).setNewsSessionCookies(response,"id","proof",3600);
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2).allMatch(cookie->cookie.contains("Secure")&&cookie.contains("HttpOnly")&&cookie.contains("Path=/admin")&&!cookie.contains("Domain="));
    }
}
