package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.example.testwigr.security.TestJwtAuthorizationFilter
import com.example.testwigr.security.JwtAuthenticationFilterMock
import com.example.testwigr.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy

@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    private String jwtSecret

    @Autowired(required = false)
    @Lazy
    private UserService userService

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers('/api/auth/**').permitAll()
                    authorize.requestMatchers('/v3/api-docs/**', '/swagger-ui/**', '/swagger-ui.html').permitAll()
                    authorize.requestMatchers('/api/users/**').permitAll()
                    // For testing, permit all requests
                    authorize.anyRequest().permitAll()
                })

        // Add a mocked JWT authentication filter that doesn't require an AuthenticationManager
        http.addFilterAt(
                new JwtAuthenticationFilterMock(jwtSecret),
                UsernamePasswordAuthenticationFilter.class
        )

        // Optionally add the JWT authorization filter if userService is available
        if (userService != null) {
            http.addFilterAfter(
                    new TestJwtAuthorizationFilter(userService, jwtSecret),
                    JwtAuthenticationFilterMock.class
            )
        }

        return http.build()
    }

    @Bean
    @Primary
    static UserDetailsService testUserDetailsService() {
        def adminDetails = User.withDefaultPasswordEncoder()
                .username('admin')
                .password('password')
                .roles('ADMIN', 'USER')
                .build()

        def testuser = User.withDefaultPasswordEncoder()
                .username('testuser')
                .password('password')
                .roles('USER')
                .build()

        def socialuser1 = User.withDefaultPasswordEncoder()
                .username('socialuser1')
                .password('password')
                .roles('USER')
                .build()

        def socialuser2 = User.withDefaultPasswordEncoder()
                .username('socialuser2')
                .password('password')
                .roles('USER')
                .build()

        def commentuser = User.withDefaultPasswordEncoder()
                .username('commentuser')
                .password('password')
                .roles('USER')
                .build()

        def postuser = User.withDefaultPasswordEncoder()
                .username('postuser')
                .password('password123')
                .roles('USER')
                .build()

        def updateuser = User.withDefaultPasswordEncoder()
                .username('updateuser')
                .password('password123')
                .roles('USER')
                .build()

        def followuser1 = User.withDefaultPasswordEncoder()
                .username('followuser1')
                .password('password123')
                .roles('USER')
                .build()

        def authuser = User.withDefaultPasswordEncoder()
                .username('authuser')
                .password('password123')
                .roles('USER')
                .build()

        def securityuser = User.withDefaultPasswordEncoder()
                .username('securityuser')
                .password('testpassword')
                .roles('USER')
                .build()

        def journeyuser = User.withDefaultPasswordEncoder()
                .username('journeyuser')
                .password('journey123')
                .roles('USER')
                .build()

        def newflowuser = User.withDefaultPasswordEncoder()
                .username('newflowuser')
                .password('flowpassword')
                .roles('USER')
                .build()

        def integrationuser = User.withDefaultPasswordEncoder()
                .username('integrationuser')
                .password('testpassword')
                .roles('USER')
                .build()

        return new InMemoryUserDetailsManager(
                adminDetails, testuser, socialuser1, socialuser2,
                commentuser, authuser, securityuser, postuser, updateuser,
                followuser1, journeyuser, newflowuser, integrationuser
        )
    }

    @Bean
    @Primary
    static PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    @Primary
    TestAuthenticationManager testAuthenticationManager() {
        return new TestAuthenticationManager()
    }

    // Custom AuthenticationManager for tests that won't throw exceptions
    static class TestAuthenticationManager implements AuthenticationManager {
        @Override
        Authentication authenticate(Authentication authentication) throws AuthenticationException {
            // For tests, always return a successful authentication with ROLE_USER
            return new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    null,
                    [new SimpleGrantedAuthority("ROLE_USER")]
            )
        }
    }
}
