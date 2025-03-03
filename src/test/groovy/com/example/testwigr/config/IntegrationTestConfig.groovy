package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@TestConfiguration
@Profile("test")
class IntegrationTestConfig {
    
    @Bean
    @Primary
    static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }
}
