package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    UserDetailsService testUserDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
            .username("testuser")
            .password("password")
            .roles("USER")
            .build()
        
        return new InMemoryUserDetailsManager(userDetails)
    }
}
