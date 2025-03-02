package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.security.JwtAuthenticationFilter
import com.example.testwigr.security.JwtAuthorizationFilter
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class UserControllerIntegrationTest extends Specification {
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        UserDetailsService testUserDetailsService(UserService userService) {
            return userService
        }
    }
    
    @Autowired
    WebTestClient webTestClient
    
    @Autowired
    UserRepository userRepository
    
    @Autowired
    PostRepository postRepository
    
    @Autowired
    PasswordEncoder passwordEncoder
    
    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
        
        // Create a test user
        def user = TestDataFactory.createUser(null, "testuser")
        user.password = passwordEncoder.encode("password")
        userRepository.save(user)
    }
    
    @WithMockUser(username = "testuser")
    def "should get user profile"() {
        when: "requesting user profile"
        def response = webTestClient
            .get()
            .uri("/api/users/testuser")
            .exchange()
        
        then: "user profile is returned"
        response.expectStatus().isOk()
            .expectBody()
            .jsonPath('$.username').isEqualTo("testuser")
            .jsonPath('$.email').isEqualTo("testuser@example.com")
    }
    
    @WithMockUser(username = "testuser")
    def "should get current user profile"() {
        when: "requesting current user profile"
        def response = webTestClient
            .get()
            .uri("/api/users/me")
            .exchange()
        
        then: "current user profile is returned"
        response.expectStatus().isOk()
            .expectBody()
            .jsonPath('$.username').isEqualTo("testuser")
    }
}
