package com.example.testwigr.controller

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestSecurityUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
class ControllerEdgeCaseTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret

    def setup() {
        // Clear database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Set up a clean security context
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        // Clean database after test
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Clean security context
        SecurityContextHolder.clearContext()
    }

    @WithMockUser(username = "postuser")
    def "should handle edge cases in post creation"() {
        given: 'A user'
        def user = TestDataFactory.createUser(null, 'postuser')
        user.password = 'password123'
        userRepository.save(user)

        when: 'Creating a post with empty content'
        def emptyPostRequest = [content: '']

        def emptyPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .with(SecurityMockMvcRequestPostProcessors.user("postuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyPostRequest))
        )

        then: 'Post creation fails with validation error'
        emptyPostResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))

        when: 'Creating a post with content exceeding maximum length'
        def longPostRequest = [content: 'X' * 300] // Exceeds 280 character limit

        def longPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .with(SecurityMockMvcRequestPostProcessors.user("postuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPostRequest))
        )

        then: 'Post creation fails with validation error'
        longPostResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))

        when: 'Creating a post with exactly the maximum length'
        def maxLengthPostRequest = [content: 'X' * 280]

        def maxLengthPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .with(SecurityMockMvcRequestPostProcessors.user("postuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxLengthPostRequest))
        )

        then: 'Post creation succeeds'
        maxLengthPostResult.andExpect(MockMvcResultMatchers.status().isOk())
    }

    @WithMockUser(username = "updateuser")
    def "should handle edge cases in post retrieval and updating"() {
        given: 'A user with a post'
        def user = TestDataFactory.createUser(null, 'updateuser')
        user.password = 'password123'
        def savedUser = userRepository.save(user)

        def post = TestDataFactory.createPost(null, 'Original post content', savedUser.id, savedUser.username)
        def savedPost = postRepository.save(post)

        when: 'Requesting a non-existent post'
        def nonExistentResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts/nonexistentid')
                        .with(SecurityMockMvcRequestPostProcessors.user("updateuser"))
        )

        then: 'Request fails with not found error'
        nonExistentResult.andExpect(MockMvcResultMatchers.status().isNotFound())

        when: 'Updating a post with valid data'
        def updateRequest = [content: 'Updated post content']

        def updateResult = mockMvc.perform(
                MockMvcRequestBuilders.put("/api/posts/${savedPost.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("updateuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        )

        then: 'Post is updated successfully'
        updateResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content').value('Updated post content'))

        // Create another user to test security
        when: 'Another user tries to update the post'
        def otherUser = TestDataFactory.createUser(null, 'otheruser')
        otherUser.password = 'password123'
        userRepository.save(otherUser)

        def unauthorizedUpdateResult = mockMvc.perform(
                MockMvcRequestBuilders.put("/api/posts/${savedPost.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("otheruser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        )

        then: 'Update fails with security exception'
        unauthorizedUpdateResult.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    @WithMockUser(username = "commentuser")
    def "should handle edge cases in comment operations"() {
        given: 'A post'
        def user = TestDataFactory.createUser(null, 'commentuser')
        user.password = 'password123'
        def savedUser = userRepository.save(user)

        def post = TestDataFactory.createPost(null, 'Post for comments', savedUser.id, savedUser.username)
        def savedPost = postRepository.save(post)

        when: 'Adding a comment with empty content'
        def emptyCommentRequest = [content: '']

        def emptyCommentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${savedPost.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("commentuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyCommentRequest))
        )

        then: 'Comment creation fails with validation error'
        emptyCommentResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))

        when: 'Adding a comment with maximum length content'
        def maxCommentRequest = [content: 'X' * 280]

        def maxCommentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${savedPost.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("commentuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxCommentRequest))
        )

        then: 'Comment is added successfully'
        maxCommentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Adding a comment to a non-existent post'
        def validCommentRequest = [content: 'Valid comment']

        def nonExistentPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/comments/posts/nonexistentid')
                        .with(SecurityMockMvcRequestPostProcessors.user("commentuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCommentRequest))
        )

        then: 'Comment creation fails with not found error'
        nonExistentPostResult.andExpect(MockMvcResultMatchers.status().isNotFound())
    }

    @WithMockUser(username = "followuser1")
    def "should handle edge cases in follow relationships"() {
        given: 'Two users'
        def user1 = TestDataFactory.createUser(null, 'followuser1')
        user1.password = 'password123'
        def savedUser1 = userRepository.save(user1)

        def user2 = TestDataFactory.createUser(null, 'followuser2')
        user2.password = 'password123'
        def savedUser2 = userRepository.save(user2)

        // Ensure we're authenticated as user1
        TestSecurityUtils.setupTestAuthentication("followuser1")

        when: 'User tries to follow themselves'
        def selfFollowResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${savedUser1.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("followuser1"))
        )

        then: 'Follow operation fails with appropriate error'
        selfFollowResult.andExpect(MockMvcResultMatchers.status().isBadRequest())

        when: 'User follows another user'
        def followResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${savedUser2.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("followuser1"))
        )

        then: 'Follow operation succeeds'
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))

        when: 'User follows the same user again'
        def duplicateFollowResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${savedUser2.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("followuser1"))
        )

        then: 'Operation succeeds but user is already following'
        duplicateFollowResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'User tries to follow a non-existent user'
        def nonExistentFollowResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/follow/nonexistentid')
                        .with(SecurityMockMvcRequestPostProcessors.user("followuser1"))
        )

        then: 'Follow operation fails with not found error'
        nonExistentFollowResult.andExpect(MockMvcResultMatchers.status().isNotFound())
    }
}
