package com.example.testwigr.integration

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestSecurityUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
@Import(TestSecurityConfig)
class BasicApiIntegrationTest extends Specification {

    @Autowired
    WebApplicationContext context

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret

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
        user.password = 'testpassword'
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

    @WithMockUser(username = "integrationuser")
    def "should authenticate and access protected resources"() {
        when: 'accessing a protected resource with mock authentication'
        def protectedResult = mockMvc.perform(
                get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.user("integrationuser"))
        )

        then: 'access is granted'
        protectedResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.username').value('integrationuser'))
    }
}
