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
    AdminSessionFilter(AdminAuthService auth, ObjectMapper json) { this.auth = auth; this.json = json; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        try {
            if (!"1".equals(request.getHeader("X-Sanctuary-Admin"))) {
                response.sendError(403); return;
            }
            auth.requireConfigured();
            String path = request.getServletPath().isEmpty() ? request.getRequestURI() : request.getServletPath();
            boolean login = "POST".equals(request.getMethod()) && (path.equals("/admin-api/login") || path.equals("/admin-api/verify"));
            if (!login) auth.authenticate(request.getHeader("Authorization"));
            chain.doFilter(request, response);
        } catch (AuthFlowException exception) {
            response.setStatus(exception.status().value()); response.setContentType("application/json");
            json.writeValue(response.getWriter(), java.util.Map.of("message", exception.getMessage()));
        }
    }
}
