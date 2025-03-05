package com.example.testwigr.config

import com.example.testwigr.security.TestJwtAuthorizationFilter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
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
import com.example.testwigr.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException

@TestConfiguration
@EnableWebSecurity
@Import(TestSecurityConfig.TestAuthenticationManager.class)
class ControllerTestSecurityConfig {

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    private String jwtSecret

    @Autowired(required = false)
    private UserService userService

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers('/api/auth/**').permitAll()
                    authorize.requestMatchers('/api/users/**').permitAll()
                    authorize.anyRequest().authenticated()
                })

        // Only add the JWT filter if userService is available
        if (userService != null) {
            http.addFilterBefore(
                    new TestJwtAuthorizationFilter(userService, jwtSecret),
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
    TestSecurityConfig.TestAuthenticationManager controllerTestAuthenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)

        return new TestSecurityConfig.TestAuthenticationManager(List.of(provider))
    }
}