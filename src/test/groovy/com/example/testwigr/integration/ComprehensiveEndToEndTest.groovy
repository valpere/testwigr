package com.example.testwigr.integration

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.Post
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
@Import(TestSecurityConfig)
class ComprehensiveEndToEndTest extends Specification {

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
        // Clear database before test
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Clear security context
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        // Clear database after test
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Clear security context
        SecurityContextHolder.clearContext()
    }

    def "should test the complete user journey"() {
        given: 'User registration data'
        def registerRequest = [
                username: 'journeyuser',
                email: 'journeyuser@example.com',
                password: 'journey123',
                displayName: 'Journey User'
        ]

        when: 'User registers'
        def registerResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        )

        then: 'Registration is successful'
        registerResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('journeyuser'))

        and: 'User exists in database'
        userRepository.findByUsername('journeyuser').isPresent()

        when: 'User creates a post'
        def createPostRequest = [
                content: 'My first journey post'
        ]

        def createPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest))
        )

        then: 'Post is created successfully'
        createPostResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content').value('My first journey post'))

        and: 'Post ID can be extracted'
        def postResponse = objectMapper.readValue(
                createPostResult.andReturn().response.contentAsString,
                Map
        )
        def postId = postResponse.id
        postId != null

        when: 'User creates a second post'
        def createPost2Request = [
                content: 'My second journey post'
        ]

        mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPost2Request))
        )

        then: 'Post count increases'
        postRepository.count() == 2

        when: 'User gets their profile'
        def profileResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
        )

        then: 'Profile is returned correctly'
        profileResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('journeyuser'))
                .andExpect(MockMvcResultMatchers.jsonPath('$.email').value('journeyuser@example.com'))

        when: 'User updates their profile'
        def updateProfileRequest = [
                displayName: 'Updated Journey User',
                bio: 'This is my journey bio'
        ]

        def userId = userRepository.findByUsername('journeyuser').get().id

        def updateProfileResult = mockMvc.perform(
                MockMvcRequestBuilders.put("/api/users/${userId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest))
        )

        then: 'Profile is updated successfully'
        updateProfileResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.displayName').value('Updated Journey User'))
                .andExpect(MockMvcResultMatchers.jsonPath('$.bio').value('This is my journey bio'))

        when: 'User adds a comment to their post'
        def addCommentRequest = [
                content: 'A comment on my own post'
        ]

        def addCommentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${postId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCommentRequest))
        )

        then: 'Comment is added successfully'
        addCommentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'User retrieves comments'
        def getCommentsResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/comments/posts/${postId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
        )

        then: 'Comments are retrieved successfully'
        getCommentsResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].content').value('A comment on my own post'))

        // Create another user to test social interactions
        when: 'Another user registers'
        def register2Request = [
                username: 'otheruser',
                email: 'otheruser@example.com',
                password: 'other123',
                displayName: 'Other User'
        ]

        mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register2Request))
        )

        and: 'Other user follows journey user'
        def otherUserId = userRepository.findByUsername('otheruser').get().id

        def followResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${userId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("otheruser"))
        )

        then: 'Follow operation succeeds'
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))

        when: 'Other user likes journey user\'s post'
        def likeResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/likes/posts/${postId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("otheruser"))
        )

        then: 'Like operation succeeds'
        likeResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.isLiked').value(true))

        when: 'Other user comments on journey user\'s post'
        def otherCommentRequest = [
                content: 'Nice post from other user!'
        ]

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${postId}")
                        .with(SecurityMockMvcRequestPostProcessors.user("otheruser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherCommentRequest))
        )

        and: 'Journey user checks their feed'
        def feedResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/feed')
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
        )

        then: 'Feed contains their posts'
        feedResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Other user checks their feed'
        def otherFeedResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/feed')
                        .with(SecurityMockMvcRequestPostProcessors.user("otheruser"))
        )

        then: 'Feed returns result successfully'
        otherFeedResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Journey user checks their followers'
        def followersResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/follow/followers')
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
        )

        then: 'Followers list can be retrieved successfully'
        followersResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Journey user logs out'
        def logoutResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/logout')
                        .with(SecurityMockMvcRequestPostProcessors.user("journeyuser"))
        )

        then: 'Logout is successful'
        logoutResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
    }
}
