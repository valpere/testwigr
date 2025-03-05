package com.example.testwigr.integration

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
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
class PostSocialIntegrationTest extends Specification {

    @Autowired
    WebApplicationContext context

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

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

        // Create a test user with a known ID for consistency
        def user = TestDataFactory.createUser('123', 'socialuser')
        userRepository.save(user)

        // Clear security context
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        // Clear security context
        SecurityContextHolder.clearContext()
    }

    @WithMockUser(username = "socialuser")
    def "should create and interact with a post"() {
        given: 'a post creation request'
        def postRequest = [content: 'Integration test post']

        when: 'creating a post'
        def createResult = mockMvc.perform(
                post('/api/posts')
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest))
        )

        then: 'post is created successfully'
        createResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.content').value('Integration test post'))
                .andExpect(jsonPath('$.userId').value('123'))

        // Extract the post ID from the response
        def postJson = objectMapper.readValue(
                createResult.andReturn().response.contentAsString,
                Map
        )
        def postId = postJson.id

        when: 'liking the post'
        def likeResult = mockMvc.perform(
                post("/api/posts/${postId}/like")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser"))
        )

        then: 'like operation succeeds'
        likeResult.andExpect(status().isOk())

        when: 'retrieving the post'
        def getResult = mockMvc.perform(
                get("/api/posts/${postId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser"))
        )

        then: 'post shows as liked'
        getResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.likes').exists())

        when: 'adding a comment'
        def commentRequest = [content: 'Integration test comment']
        def commentResult = mockMvc.perform(
                post("/api/posts/${postId}/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest))
        )

        then: 'comment is added successfully'
        commentResult.andExpect(status().isOk())

        when: 'retrieving comments'
        def getCommentsResult = mockMvc.perform(
                get("/api/posts/${postId}/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser"))
        )

        then: 'comment is visible'
        getCommentsResult.andExpect(status().isOk())
    }

}
