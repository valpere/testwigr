package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles

@TestConfiguration
@ActiveProfiles("test")
class TestMongoConfig {
    // This class enables the embedded MongoDB configuration
    // The actual configuration happens through Spring Boot's auto-configuration
}
