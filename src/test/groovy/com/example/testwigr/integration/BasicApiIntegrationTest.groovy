package com.example.testwigr.integration

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
class BasicApiIntegrationTest extends Specification {

    @Autowired
    WebApplicationContext context

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build()

        // Clean up repositories
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user
        def user = TestDataFactory.createUser(null, 'integrationuser')
        user.password = passwordEncoder.encode('testpassword')
        userRepository.save(user)
    }

    def "should register a new user"() {
        given:
        def registrationRequest = [
            username: 'newuser',
            email: 'newuser@example.com',
            password: 'password123',
            displayName: 'New User'
        ]

        when:
        def result = mockMvc.perform(
            post('/api/auth/register')
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.success').value(true))
              .andExpect(jsonPath('$.username').value('newuser'))

        and:
        userRepository.findByUsername('newuser').isPresent()
    }

    def "should authenticate and access protected resources"() {
        given:
        def loginRequest = [
            username: 'integrationuser',
            password: 'testpassword'
        ]

        when: 'user logs in'
        def loginResult = mockMvc.perform(
            post('/api/auth/login')
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: 'authentication succeeds'
        loginResult.andExpect(status().isOk())
                 .andExpect(jsonPath('$.token').exists())

        and: 'token can be extracted'
        def responseJson = objectMapper.readValue(
            loginResult.andReturn().response.contentAsString,
            Map
        )
        def token = responseJson.token

        when: 'accessing a protected resource with the token'
        def protectedResult = mockMvc.perform(
            get('/api/users/me')
                .header('Authorization', "Bearer ${token}")
        )

        then: 'access is granted'
        protectedResult.andExpect(status().isOk())
                     .andExpect(jsonPath('$.username').value('integrationuser'))
    }

}
