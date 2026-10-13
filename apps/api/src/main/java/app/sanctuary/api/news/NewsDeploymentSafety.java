package app.sanctuary.api.news;

import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.config.WebProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Refuse a production boot with settings that undermine administrator isolation. */
@Component
@Profile("prod")
public class NewsDeploymentSafety {
    public NewsDeploymentSafety(AuthProperties auth, WebProperties web,
                                @org.springframework.beans.factory.annotation.Value("${spring.datasource.url}") String jdbcUrl) {
        if (!auth.enabled() || !auth.cookie().secure())
            throw new IllegalStateException("Production requires authentication and Secure cookies");
        if (auth.clientId() == null || auth.clientId().isBlank()
            || auth.userPoolId() == null || auth.userPoolId().isBlank())
            throw new IllegalStateException("Production requires a Cognito pool and app client");
        if (!java.util.Set.of("None", "Lax", "Strict").contains(auth.cookie().sameSite()))
            throw new IllegalStateException("Invalid cookie SameSite configuration");
        if (web.allowedOrigins().isEmpty() || web.allowedOrigins().stream().anyMatch(origin -> {
            try {
                var uri = java.net.URI.create(origin);
                return !"https".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty()) || origin.contains("*");
            } catch (IllegalArgumentException exception) { return true; }
        })) throw new IllegalStateException("Production requires explicit HTTPS CORS origins");
        requireVerifiedDatabaseTls(jdbcUrl);
    }

    static void requireVerifiedDatabaseTls(String jdbcUrl) {
        try {
            if(!jdbcUrl.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException();
            var uri=java.net.URI.create(jdbcUrl.substring(5));
            if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null)throw new IllegalArgumentException();
            var values=new java.util.HashMap<String,String>();
            for(String pair:uri.getRawQuery().split("&")) {
                String[] parts=pair.split("=",2);
                String name=java.net.URLDecoder.decode(parts[0],java.nio.charset.StandardCharsets.UTF_8);
                String value=parts.length==2?java.net.URLDecoder.decode(parts[1],java.nio.charset.StandardCharsets.UTF_8):"";
                if(name.toLowerCase(java.util.Locale.ROOT).startsWith("ssl") && !name.equals("sslmode") && !name.equals("sslrootcert"))throw new IllegalArgumentException();
                if(values.putIfAbsent(name,value)!=null)throw new IllegalArgumentException();
            }
            if(!"verify-full".equals(values.get("sslmode")) || !"/app/rds-ca.pem".equals(values.get("sslrootcert")))
                throw new IllegalArgumentException();
        } catch(RuntimeException exception) {
            throw new IllegalStateException("Production requires verified PostgreSQL TLS and the bundled RDS CA");
        }
    }
}
