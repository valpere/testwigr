package com.example.testwigr.integration

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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
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
    }

    def cleanup() {
        // Clear database after test
        postRepository.deleteAll()
        userRepository.deleteAll()
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

        when: 'User logs in'
        def loginRequest = [
            username: 'journeyuser',
            password: 'journey123'
        ]

        def loginResult = mockMvc.perform(
            MockMvcRequestBuilders.post('/api/auth/login')
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: 'Login is successful and token is returned'
        loginResult.andExpect(MockMvcResultMatchers.status().isOk())

        and: 'Token can be extracted'
        def loginResponse = objectMapper.readValue(
            loginResult.andReturn().response.contentAsString,
            Map
        )
        def token = loginResponse.token
        token != null

        when: 'User creates a post'
        def createPostRequest = [
            content: 'My first journey post'
        ]

        def createPostResult = mockMvc.perform(
            MockMvcRequestBuilders.post('/api/posts')
                .header('Authorization', "Bearer ${token}")
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
                .header('Authorization', "Bearer ${token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPost2Request))
        )

        then: 'Post count increases'
        postRepository.count() == 2

        when: 'User gets their profile'
        def profileResult = mockMvc.perform(
            MockMvcRequestBuilders.get('/api/users/me')
                .header('Authorization', "Bearer ${token}")
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
                .header('Authorization', "Bearer ${token}")
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
                .header('Authorization', "Bearer ${token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addCommentRequest))
        )

        then: 'Comment is added successfully'
        addCommentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'User retrieves comments'
        def getCommentsResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/comments/posts/${postId}")
                .header('Authorization', "Bearer ${token}")
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

        and: 'Other user logs in'
        def login2Request = [
            username: 'otheruser',
            password: 'other123'
        ]

        def login2Result = mockMvc.perform(
            MockMvcRequestBuilders.post('/api/auth/login')
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login2Request))
        )

        then: 'Login is successful'
        def login2Response = objectMapper.readValue(
            login2Result.andReturn().response.contentAsString,
            Map
        )
        def token2 = login2Response.token
        token2 != null

        when: 'Other user follows journey user'
        def otherUserId = userRepository.findByUsername('otheruser').get().id

        def followResult = mockMvc.perform(
           MockMvcRequestBuilders.post("/api/follow/${userId}")
               .header('Authorization', "Bearer ${token2}")
       )

       then: 'Follow operation succeeds'
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
           .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))

       when: 'Other user likes journey user\'s post'
        def likeResult = mockMvc.perform(
           MockMvcRequestBuilders.post("/api/likes/posts/${postId}")
               .header('Authorization', "Bearer ${token2}")
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
               .header('Authorization', "Bearer ${token2}")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(otherCommentRequest))
       )

       and: 'Journey user checks their feed'
        def feedResult = mockMvc.perform(
           MockMvcRequestBuilders.get('/api/feed')
               .header('Authorization', "Bearer ${token}")
       )

       then: 'Feed contains their posts'
        feedResult.andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath('$.content').isArray())
           .andExpect(MockMvcResultMatchers.jsonPath('$.content.length()').value(2))

       when: 'Other user checks their feed'
        def otherFeedResult = mockMvc.perform(
           MockMvcRequestBuilders.get('/api/feed')
               .header('Authorization', "Bearer ${token2}")
       )

       then: 'Feed contains journey user\'s posts'
        otherFeedResult.andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath('$.content').isArray())
           .andExpect(MockMvcResultMatchers.jsonPath('$.content.length()').value(2))

       when: 'Journey user checks their followers'
        def followersResult = mockMvc.perform(
           MockMvcRequestBuilders.get('/api/follow/followers')
               .header('Authorization', "Bearer ${token}")
       )

       then: 'Followers include other user'
        followersResult.andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath('$[0].username').value('otheruser'))

       when: 'Journey user logs out'
        def logoutResult = mockMvc.perform(
           MockMvcRequestBuilders.post('/api/auth/logout')
               .header('Authorization', "Bearer ${token}")
       )

       then: 'Logout is successful'
        logoutResult.andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
    }

}
