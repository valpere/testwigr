package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.example.testwigr.security.JwtAuthorizationFilter
import com.example.testwigr.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration

@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    private String jwtSecret

    @Autowired(required = false)
    private UserService userService

    @Bean
    @Primary
    SecurityFilterChain testFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers('/api/auth/**').permitAll()
                authorize.requestMatchers('/api/users/**').permitAll()
                authorize.anyRequest().authenticated()
            })

        // Only add the JwtAuthorizationFilter if both authenticationManager and userService are available
        if (authenticationManager != null && userService != null) {
            http.addFilterBefore(
                new JwtAuthorizationFilter(authenticationManager, userService, jwtSecret),
                UsernamePasswordAuthenticationFilter.class
            )
        }

        return http.build()
    }

    @Bean
    @Primary
    static UserDetailsService testUserDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
            .username('testuser')
            .password('password')
            .roles('USER')
            .build()

        def adminDetails = User.withDefaultPasswordEncoder()
            .username('admin')
            .password('password')
            .roles('ADMIN', 'USER')
            .build()

        return new InMemoryUserDetailsManager(userDetails, adminDetails)
    }

    @Bean
    @Primary
    static PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    @Primary
    static AuthenticationManager testAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager()
    }

}
