package app.sanctuary.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OriginEnforcementFilterTest {

    @Test
    void rejectsUnsafeRequestFromUntrustedOrigin() throws Exception {
        WebProperties properties = new WebProperties();
        properties.setAllowedOrigins(List.of("https://mydailysanctuary.com"));
        OriginEnforcementFilter filter = new OriginEnforcementFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/me/preferences");
        request.addHeader("Origin", "https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Origin is not allowed");
    }

    @Test
    void allowsUnsafeRequestFromTrustedOrigin() throws Exception {
        WebProperties properties = new WebProperties();
        properties.setAllowedOrigins(List.of("https://mydailysanctuary.com"));
        OriginEnforcementFilter filter = new OriginEnforcementFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/me/preferences");
        request.addHeader("Origin", "https://mydailysanctuary.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsRequestsWithoutOriginForMobileClients() throws Exception {
        WebProperties properties = new WebProperties();
        properties.setAllowedOrigins(List.of("https://mydailysanctuary.com"));
        OriginEnforcementFilter filter = new OriginEnforcementFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/me/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresSafeReadRequests() throws Exception {
        WebProperties properties = new WebProperties();
        properties.setAllowedOrigins(List.of("https://mydailysanctuary.com"));
        OriginEnforcementFilter filter = new OriginEnforcementFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/content/saints/search");
        request.addHeader("Origin", "https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
