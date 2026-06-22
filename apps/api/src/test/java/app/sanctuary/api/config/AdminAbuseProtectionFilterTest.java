package app.sanctuary.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminAbuseProtectionFilterTest {

    @Test
    void limitsAdminRequestsByForwardedIp() throws Exception {
        AdminAbuseProtectionProperties properties = new AdminAbuseProtectionProperties();
        properties.setRequestLimit(new AdminAbuseProtectionProperties.Limit(1, Duration.ofMinutes(1)));
        AdminAbuseProtectionFilter filter = new AdminAbuseProtectionFilter(properties);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(adminRequest(), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(adminRequest(), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("Too many admin requests");
    }

    @Test
    void ignoresNonAdminRoutes() throws Exception {
        AdminAbuseProtectionProperties properties = new AdminAbuseProtectionProperties();
        properties.setRequestLimit(new AdminAbuseProtectionProperties.Limit(0, Duration.ofMinutes(1)));
        AdminAbuseProtectionFilter filter = new AdminAbuseProtectionFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest adminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/users");
        request.addHeader("X-Forwarded-For", "203.0.113.20, 10.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }
}
