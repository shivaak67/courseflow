package com.prioritize;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PrioritizeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrioritizeApplication.class, args);
    }
}
