package com.example.testwigr.security

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
@Import(TestSecurityConfig)
class AuthenticationFlowIntegrationTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret

    def setup() {
        userRepository.deleteAll()

        // Create various test users with different states
        def activeUser = TestDataFactory.createUser(null, 'activeuser')
        activeUser.active = true
        userRepository.save(activeUser)

        def inactiveUser = TestDataFactory.createUser(null, 'inactiveuser')
        inactiveUser.active = false
        userRepository.save(inactiveUser)
    }

    def "should block access with expired token"() {
        given: 'An expired JWT token'
        def expiredToken = generateExpiredToken('activeuser', jwtSecret)

        when: 'Accessing protected resource with expired token'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts')
                        .header('Authorization', "Bearer ${expiredToken}")
        )

        then: 'Access is denied with 403 status'
        result.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    def "should reject login for inactive user"() {
        given: 'Login request for inactive user'
        def loginRequest = [
                username: 'inactiveuser',
                password: 'password123'
        ]

        when: 'Attempting to login'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: 'Login is rejected'
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    def "should complete full authentication flow"() {
        given: 'Registration data'
        def registerRequest = [
                username: 'newflowuser',
                email: 'newflowuser@example.com',
                password: 'flowpassword',
                displayName: 'Flow User'
        ]

        when: 'Registering a new user'
        def registerResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        )

        then: 'Registration succeeds'
        registerResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))

        when: 'Accessing protected resource with mock authentication'
        def protectedResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.user("newflowuser"))
        )

        then: 'Access is granted'
        protectedResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('newflowuser'))

        when: 'Logging out'
        def logoutResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/logout')
                        .with(SecurityMockMvcRequestPostProcessors.user("newflowuser"))
        )

        then: 'Logout succeeds'
        logoutResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
    }

    // Helper method to generate expired JWT token
    private String generateExpiredToken(String username, String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS)

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(past.minus(2, ChronoUnit.DAYS)))
                .expiration(Date.from(past))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact()
    }
}
