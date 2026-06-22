package app.sanctuary.api.auth.service;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import app.sanctuary.api.auth.dto.AuthSessionResponse;
import app.sanctuary.api.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class WebAuthCookieService {
    public static final String ID_COOKIE = "sanctuary_id";
    public static final String REFRESH_COOKIE = "sanctuary_refresh";

    private final AuthProperties authProperties;

    public WebAuthCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void setSessionCookies(HttpServletResponse response, AuthSessionResponse session) {
        addCookie(response, ID_COOKIE, "", "/me", Duration.ZERO);
        addCookie(response, ID_COOKIE, session.idToken(), "/", Duration.ofSeconds(session.expiresIn()));
        if (session.refreshToken() != null && !session.refreshToken().isBlank()) {
            addCookie(
                response,
                REFRESH_COOKIE,
                session.refreshToken(),
                "/auth/web/refresh",
                Duration.ofDays(authProperties.cookie().refreshMaxAgeDays())
            );
        }
    }

    public void clearSessionCookies(HttpServletResponse response) {
        addCookie(response, ID_COOKIE, "", "/", Duration.ZERO);
        addCookie(response, ID_COOKIE, "", "/me", Duration.ZERO);
        addCookie(response, REFRESH_COOKIE, "", "/auth/web/refresh", Duration.ZERO);
    }

    public String refreshToken(HttpServletRequest request) {
        return cookieValue(request, REFRESH_COOKIE);
    }

    public static String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        AuthProperties.Cookie cookieProperties = authProperties.cookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(cookieProperties.secure())
            .sameSite(cookieProperties.sameSite())
            .path(path)
            .maxAge(maxAge);

        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
