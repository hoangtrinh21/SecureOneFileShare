package com.fileshare.oneshot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OneShotApplication {

    public static void main(String[] args) {
        SpringApplication.run(OneShotApplication.class, args);
    }
}
