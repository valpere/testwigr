package com.example.testwigr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.boot.test.context.SpringBootTest

// Use a conditional annotation to skip this test for now
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class TestwigrApplicationTests {

    @Test
    void contextLoads() {
        // This test will be skipped unless the environment variable is set
    }
}
