package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.ActiveProfiles

@TestConfiguration
@ActiveProfiles("test")
class TestMongoConfig {
    // This class is now just a marker for test configurations
    // The actual MongoDB connection happens through the Docker container
}