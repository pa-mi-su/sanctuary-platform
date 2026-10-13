package app.sanctuary.api.news;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bound JSON before deserialization, including chunked requests without Content-Length. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class NewsRequestLimitsFilter extends OncePerRequestFilter {
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI();
        return !(path.equals("/admin/news") || path.startsWith("/admin/news/") || path.startsWith("/auth/news/"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
        throws IOException,ServletException {
        response.setHeader("Cache-Control","no-store");
        response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Content-Security-Policy","default-src 'none'; frame-ancestors 'none'");
        response.setHeader("Referrer-Policy","no-referrer");
        String contentType=request.getContentType();
        if (!Set.of("POST","PUT","PATCH").contains(request.getMethod())
            || contentType==null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            chain.doFilter(request,response);return;
        }
        int maximum=request.getRequestURI().startsWith("/auth/news/")?16384:524288;
        if(request.getContentLengthLong()>maximum){tooLarge(response);return;}
        byte[] bytes=request.getInputStream().readNBytes(maximum+1);
        if(bytes.length>maximum){tooLarge(response);return;}
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public ServletInputStream getInputStream(){
                var input=new ByteArrayInputStream(bytes);
                return new ServletInputStream(){
                    @Override public int read(){return input.read();}
                    @Override public int read(byte[] b,int off,int len){return input.read(b,off,len);}
                    @Override public boolean isFinished(){return input.available()==0;}
                    @Override public boolean isReady(){return true;}
                    @Override public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Synchronous requests only");}
                };
            }
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        },response);
    }

    private void tooLarge(HttpServletResponse response)throws IOException {
        response.setStatus(413);response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Request is too large.\"}");
    }
}
