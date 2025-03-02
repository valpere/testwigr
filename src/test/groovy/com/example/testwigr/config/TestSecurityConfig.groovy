package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
@Profile('test')
class TestSecurityConfig {

    @Bean
    @Primary
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())

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
    PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder()
    }

}
