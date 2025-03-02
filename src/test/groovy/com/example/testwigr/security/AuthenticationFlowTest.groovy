package com.example.testwigr.security

import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AuthenticationFlowTest extends Specification {
    
    @Autowired
    WebTestClient webTestClient
    
    @Autowired
    UserRepository userRepository
    
    @Autowired
    PasswordEncoder passwordEncoder
    
    @Autowired
    ObjectMapper objectMapper
    
    def setup() {
        userRepository.deleteAll()
        
        // Create a test user with known credentials
        def user = TestDataFactory.createUser(null, "authuser")
        user.password = passwordEncoder.encode("password123")
        userRepository.save(user)
    }
    
    def "should register a new user"() {
        given: "registration request data"
        def requestBody = [
            username: "newuser",
            email: "newuser@example.com",
            password: "newpassword",
            displayName: "New User"
        ]
        
        when: "sending registration request"
        def response = webTestClient
            .post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(requestBody))
            .exchange()
        
        then: "user is registered successfully"
        response.expectStatus().isOk()
            .expectBody()
            .jsonPath('$.success').isEqualTo(true)
            .jsonPath('$.username').isEqualTo("newuser")
        
        and: "user exists in the database"
        userRepository.findByUsername("newuser").isPresent()
    }
    
    def "should login successfully and get JWT token"() {
        given: "login request data"
        def requestBody = [
            username: "authuser",
            password: "password123"
        ]
        
        when: "sending login request"
        def response = webTestClient
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(requestBody))
            .exchange()
        
        then: "login is successful and token is returned"
        response.expectStatus().isOk()
            .expectBody()
            .jsonPath('$.token').exists()
            .jsonPath('$.username').isEqualTo("authuser")
    }
    
    def "should reject login with incorrect credentials"() {
        given: "incorrect login request data"
        def requestBody = [
            username: "authuser",
            password: "wrongpassword"
        ]
        
        when: "sending login request with incorrect password"
        def response = webTestClient
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(requestBody))
            .exchange()
        
        then: "login is rejected"
        response.expectStatus().isUnauthorized()
    }
}
