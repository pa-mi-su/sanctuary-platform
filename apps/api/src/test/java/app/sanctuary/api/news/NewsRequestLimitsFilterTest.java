package app.sanctuary.api.news;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class NewsRequestLimitsFilterTest {
    @Test void rejectsOversizedChunkedAuthenticationBodyBeforeController()throws Exception {
        var request=new MockHttpServletRequest("POST","/auth/news/login"){
            @Override public long getContentLengthLong(){return -1;}
        };
        request.setContentType("application/json");request.setContent(new byte[16385]);
        var chain=new MockFilterChain();var response=new MockHttpServletResponse();
        new NewsRequestLimitsFilter().doFilter(request,response,chain);
        assertThat(response.getStatus()).isEqualTo(413);assertThat(chain.getRequest()).isNull();
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }
    @Test void validJsonRemainsReadableForController()throws Exception {
        var request=new MockHttpServletRequest("POST","/auth/news/verify");
        request.setContentType("application/json");request.setContent("{\"code\":\"123456\"}".getBytes());
        var response=new MockHttpServletResponse();var chain=new MockFilterChain();
        new NewsRequestLimitsFilter().doFilter(request,response,chain);
        assertThat(chain.getRequest().getReader().readLine()).isEqualTo("{\"code\":\"123456\"}");
    }
}
