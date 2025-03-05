package com.example.testwigr.security

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
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
class AuthenticationFlowTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper

    def setup() {
        userRepository.deleteAll()

        // Create a test user with known credentials
        def user = TestDataFactory.createUser(null, 'authuser')
        user.password = passwordEncoder.encode('password123')
        userRepository.save(user)
    }

    def "should register a new user"() {
        given: 'registration request data'
        def requestBody = [
                username: 'newuser',
                email: 'newuser@example.com',
                password: 'newpassword',
                displayName: 'New User'
        ]

        when: 'sending registration request'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
        )

        then: 'user is registered successfully'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('newuser'))

        and: 'user exists in the database'
        userRepository.findByUsername('newuser').isPresent()
    }

    @WithMockUser(username = "authuser")
    def "should login successfully and get JWT token"() {
        when: 'requesting profile data after authentication'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.user("authuser"))
        )

        then: 'user profile is returned'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('authuser'))
    }

    def "should reject login with incorrect credentials"() {
        given: 'incorrect login request data'
        def requestBody = [
                username: 'authuser',
                password: 'wrongpassword'
        ]

        when: 'sending login request with incorrect password'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
        )

        then: 'login is rejected'
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }
}
