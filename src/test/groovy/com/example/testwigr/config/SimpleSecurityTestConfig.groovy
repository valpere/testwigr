package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
@EnableWebSecurity
class SimpleSecurityTestConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers('/api/auth/**').permitAll()
                authorize.requestMatchers('/api/users/**').permitAll()
                // For testing, we'll allow all requests to simplify
                authorize.anyRequest().permitAll()
            })

        return http.build()
    }

    @Bean
    UserDetailsService userDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
            .username('testuser')
            .password('password')
            .roles('USER')
            .build()

        return new InMemoryUserDetailsManager(userDetails)
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }
}
