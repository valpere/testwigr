package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {

    @Bean
    @Primary
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers('/api/auth/**').permitAll()
                authorize.requestMatchers('/api/users/**').permitAll()
                authorize.anyRequest().authenticated()
            })

        return http.build()
    }

    @Bean
    @Primary
    UserDetailsService testUserDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
            .username('testuser')
            .password('password')
            .roles('USER')
            .build()

        return new InMemoryUserDetailsManager(userDetails)
    }
    
    @Bean
    @Primary
    org.springframework.security.crypto.password.PasswordEncoder testPasswordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
    }

}
