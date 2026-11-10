package app.sanctuary.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AdminSessionCookies {
    public static final String NAME = "sanctuary_admin";
    private final boolean secure;
    public AdminSessionCookies(@Value("${sanctuary.admin.cookie-secure:true}") boolean secure) { this.secure = secure; }
    public String issue(String token) { return cookie(token, 24 * 60 * 60); }
    public String clear() { return cookie("", 0); }
    private String cookie(String value, int seconds) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict")
            .path("/admin-api").maxAge(seconds).build().toString();
    }
    public static String authorization(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null) return header;
        if (request.getCookies() != null) for (var cookie : request.getCookies())
            if (NAME.equals(cookie.getName())) return "Bearer " + cookie.getValue();
        return null;
    }
}
