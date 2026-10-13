package app.sanctuary.api.news;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewsUploadTest {
    @Test void rejectsSvgAndOversizedUploads() {
        var repo=mock(NewsRepository.class);var controller=new NewsController(repo);
        assertThatThrownBy(()->controller.upload(new MockMultipartFile("file","a.svg","image/svg+xml","<svg onload='alert(1)'/>".getBytes()))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->controller.upload(new MockMultipartFile("file","a.png","image/png",new byte[5242881]))).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repo);
    }
    @Test void validatesDimensionsAndReencodesValidPng() throws Exception {
        var repo=mock(NewsRepository.class);var controller=new NewsController(repo);
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(6001,1,BufferedImage.TYPE_INT_RGB),"png",output);
        assertThatThrownBy(()->controller.upload(new MockMultipartFile("file","a.png","image/png",output.toByteArray()))).isInstanceOf(ResponseStatusException.class);
        output.reset();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",output);
        when(repo.image(eq("image/png"),any())).thenReturn(java.util.UUID.randomUUID());
        assertThat(controller.upload(new MockMultipartFile("file","photo.png","image/png",output.toByteArray()))).containsKey("id");
        verify(repo).image(eq("image/png"),any());
    }
}
