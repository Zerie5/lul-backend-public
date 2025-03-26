package com.lul;  // Match your main application package

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LulBackendApplicationTests {

    @Test
    void contextLoads() {
    }
} 