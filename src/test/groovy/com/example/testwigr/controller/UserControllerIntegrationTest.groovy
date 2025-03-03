package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class UserControllerIntegrationTest extends Specification {

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        PasswordEncoder testPasswordEncoder() {
            return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
        }

    }

    @Autowired
    MockMvc mockMvc

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
        def user = TestDataFactory.createUser(null, 'testuser')
        user.password = passwordEncoder.encode('password')
        userRepository.save(user)
    }

    @WithMockUser(username = 'testuser')
    def "should get user profile"() {
        when: 'requesting user profile'
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get('/api/users/testuser')
        )

        then: 'user profile is returned'
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
              .andExpect(MockMvcResultMatchers.jsonPath('$.email').value('testuser@example.com'))
    }

    @WithMockUser(username = 'testuser')
    def "should get current user profile"() {
        when: 'requesting current user profile'
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get('/api/users/me')
        )

        then: 'current user profile is returned'
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
    }

}
