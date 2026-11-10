package app.sanctuary.api.admin;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.sanctuary.api.auth.service.AuthFlowException;

/** All admin routes fail closed, including future controllers and unknown paths. */
final class AdminSessionFilter extends OncePerRequestFilter {
    private final AdminAuthService auth;
    private final ObjectMapper json;
    private final AdminSessionCookies cookies;
    AdminSessionFilter(AdminAuthService auth, ObjectMapper json) { this(auth, json, new AdminSessionCookies(true)); }
    AdminSessionFilter(AdminAuthService auth, ObjectMapper json, AdminSessionCookies cookies) { this.auth = auth; this.json = json; this.cookies = cookies; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        try {
            if (!"1".equals(request.getHeader("X-Sanctuary-Admin"))) {
                response.setStatus(403); return;
            }
            auth.requireConfigured();
            String path = request.getServletPath().isEmpty() ? request.getRequestURI() : request.getServletPath();
            boolean login = "POST".equals(request.getMethod()) && (path.equals("/admin-api/login") || path.equals("/admin-api/verify") || path.equals("/admin-api/password"));
            String authorization = AdminSessionCookies.authorization(request);
            if (!login) auth.authenticate(authorization);
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override public String getHeader(String name) {
                    return "Authorization".equalsIgnoreCase(name) ? authorization : super.getHeader(name);
                }
                @Override public java.util.Enumeration<String> getHeaders(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) return java.util.Collections.enumeration(authorization == null ? java.util.List.of() : java.util.List.of(authorization));
                    return super.getHeaders(name);
                }
            };
            chain.doFilter(wrapped, response);
        } catch (AuthFlowException exception) {
            if (exception.status().value() == 401) response.addHeader("Set-Cookie", cookies.clear());
            response.setStatus(exception.status().value()); response.setContentType("application/json");
            json.writeValue(response.getWriter(), java.util.Map.of("message", exception.getMessage()));
        }
    }
}
