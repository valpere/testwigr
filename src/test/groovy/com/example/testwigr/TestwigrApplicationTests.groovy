package com.example.testwigr

import com.example.testwigr.config.TestMongoConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestMongoConfig.class)
@ActiveProfiles("test")
class TestwigrApplicationTests {

    @Test
    void contextLoads() {
        // Just loads the Spring context
    }
}
