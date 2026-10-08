package app.sanctuary.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SanctuaryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SanctuaryApiApplication.class, args);
    }
}
