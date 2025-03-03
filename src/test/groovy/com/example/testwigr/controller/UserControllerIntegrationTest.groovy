package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
class UserControllerIntegrationTest extends ControllerTestBase {

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
            withAuth(MockMvcRequestBuilders.get('/api/users/testuser'))
        )

        then: 'user profile is returned'
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
              .andExpect(MockMvcResultMatchers.jsonPath('$.email').value('testuser@example.com'))
    }

    def "should get current user profile"() {
        when: 'requesting current user profile with valid auth token'
        def result = mockMvc.perform(
            withAuth(MockMvcRequestBuilders.get('/api/users/me'))
        )

        then: 'current user profile is returned'
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
    }

    def "should update current user profile"() {
        given: 'update data'
        def updateRequest = [
            displayName: 'Updated Name',
            bio: 'New bio text'
        ]

        when: 'updating current user profile with valid auth token'
        def result = mockMvc.perform(
            jsonRequest(
                withAuth(MockMvcRequestBuilders.put('/api/users/me')),
                updateRequest
            )
        )

        then: 'user profile is updated correctly'
        result.andExpect(MockMvcResultMatchers.status().isOk())
              .andExpect(MockMvcResultMatchers.jsonPath('$.displayName').value('Updated Name'))
              .andExpect(MockMvcResultMatchers.jsonPath('$.bio').value('New bio text'))
    }

}
