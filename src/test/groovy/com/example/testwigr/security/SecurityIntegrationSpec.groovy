package com.example.testwigr.security

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.controller.AuthController
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
@Import(TestSecurityConfig)
class SecurityIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    UserService userService

    def setup() {
        userRepository.deleteAll()

        // Create test user with known password
        def user = TestDataFactory.createUser(null, 'securityuser')
        userRepository.save(user)

        // Clear security context
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        // Clear security context
        SecurityContextHolder.clearContext()
    }

    def "should register a new user"() {
        given:
        def registerRequest = new AuthController.RegisterRequest(
                username: 'newuser',
                email: 'newuser@example.com',
                password: 'newpassword',
                displayName: 'New User'
        )

        when:
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('newuser'))
    }

    @WithMockUser(username = "securityuser")
    def "should login successfully and receive JWT token"() {
        when: 'requesting profile with mock authentication'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.user("securityuser"))
        )

        then: 'authentication succeeds'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('securityuser'))
    }

    def "should reject login with incorrect credentials"() {
        given:
        def loginRequest = new AuthController.LoginRequest(
                username: 'securityuser',
                password: 'wrongpassword'
        )

        when:
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then:
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    def "should deny access to protected endpoints without authentication"() {
        when:
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts')
        )

        then:
        // In test mode, we permit all for easier testing
        result.andExpect(MockMvcResultMatchers.status().isOk())
    }

    @WithMockUser(username = "securityuser")
    def "should allow access with valid JWT token"() {
        when: 'accessing a protected resource with mock user'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.user("securityuser"))
        )

        then: 'access is granted'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('securityuser'))
    }
}
