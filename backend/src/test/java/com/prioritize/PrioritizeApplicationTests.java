package com.prioritize;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PrioritizeApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context, security, and Flyway start with the test profile.
    }
}
