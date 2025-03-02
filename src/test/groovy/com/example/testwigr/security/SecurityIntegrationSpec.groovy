package com.example.testwigr.security

import com.example.testwigr.controller.AuthController
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    UserService userService

    @Autowired
    PasswordEncoder passwordEncoder

    def setup() {
        userRepository.deleteAll()
        
        // Create test user with known password
        def user = TestDataFactory.createUser(null, "securityuser")
        user.password = passwordEncoder.encode("testpassword")
        userRepository.save(user)
    }

    def "should register a new user"() {
        given:
        def registerRequest = new AuthController.RegisterRequest(
            username: "newuser",
            email: "newuser@example.com",
            password: "newpassword",
            displayName: "New User"
        )

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
            .andExpect(MockMvcResultMatchers.jsonPath('$.username').value("newuser"))
    }

    def "should login successfully and receive JWT token"() {
        given:
        def loginRequest = new AuthController.LoginRequest(
            username: "securityuser",
            password: "testpassword"
        )

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.token').exists())
            .andExpect(MockMvcResultMatchers.jsonPath('$.username').value("securityuser"))
    }

    def "should reject login with incorrect credentials"() {
        given:
        def loginRequest = new AuthController.LoginRequest(
            username: "securityuser",
            password: "wrongpassword"
        )

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    def "should deny access to protected endpoints without authentication"() {
        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/posts")
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    def "should allow access with valid JWT token"() {
        given:
        // First login to get a token
        def loginRequest = new AuthController.LoginRequest(
            username: "securityuser",
            password: "testpassword"
        )
        
        def loginResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
        
        def token = objectMapper.readValue(
            loginResult.andReturn().response.contentAsString,
            Map
        ).token
        
        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header("Authorization", "Bearer ${token}")
        )
        
        then:
        result.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.username').value("securityuser"))
    }
}
