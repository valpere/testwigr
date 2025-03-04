package com.example.testwigr.integration

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestDatabaseUtils
import com.example.testwigr.test.TestSecurityUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class SocialNetworkIntegrationTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper
    
    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret
    
    // Keep track of users and their tokens
    Map<String, String> userTokens = [:]
    List<User> testUsers = []
    
    def setup() {
        // Start with a clean database
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
        
        // Create test users directly without using the login endpoint
        5.times { i ->
            def username = "socialuser${i}"
            
            // Create and save user
            def user = TestDataFactory.createUser(null, username)
            user.password = passwordEncoder.encode("password123")
            def savedUser = userRepository.save(user)
            testUsers << savedUser
            
            // Generate token using TestSecurityUtils
            userTokens[username] = TestSecurityUtils.generateTestToken(username, jwtSecret)
        }
    }
    
    def cleanup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    def "should test basic social network interactions"() {
        given: 'A network of users'
        def posts = []
        
        when: 'Each user creates a post'
        testUsers.each { user ->
            def username = user.username
            def token = userTokens[username]
            
            // Use a plain Java String instead of GString to avoid serialization issues
            // Note the single quotes and explicit toString() call
            String postContent = "Test post from " + username
            def createPostRequest = [content: postContent]
            
            try {
                def result = mockMvc.perform(
                    MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest))
                ).andReturn()
                
                if (result.response.status == 200) {
                    def post = objectMapper.readValue(result.response.contentAsString, Map)
                    posts << [id: post.id, authorId: user.id]
                } else {
                    println "Failed to create post. Status: ${result.response.status}"
                    println "Response: ${result.response.contentAsString}"
                }
            } catch (Exception e) {
                println "Error creating post for ${username}: ${e.message}"
            }
        }
        
        then: 'Posts are created successfully'
        !posts.isEmpty()
        
        when: 'Users follow each other'
        def followCount = 0
        // Each user follows the next user in the list (circular)
        testUsers.eachWithIndex { user, index ->
            def nextIndex = (index + 1) % testUsers.size()
            def followeeId = testUsers[nextIndex].id
            
            try {
                def result = mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/follow/${followeeId}")
                        .header('Authorization', "Bearer ${userTokens[user.username]}")
                ).andReturn()
                
                if (result.response.status == 200) {
                    followCount++
                }
            } catch (Exception e) {
                println "Error in follow operation: ${e.message}"
            }
        }
        
        then: 'Follow operations succeed'
        followCount > 0
        
        when: 'Users like posts'
        def likeCount = 0
        // Each user likes all available posts
        if (!posts.isEmpty()) {
            testUsers.each { user ->
                posts.each { post ->
                    // Don't like your own posts
                    if (post.authorId != user.id) {
                        try {
                            def result = mockMvc.perform(
                                MockMvcRequestBuilders.post("/api/likes/posts/${post.id}")
                                    .header('Authorization', "Bearer ${userTokens[user.username]}")
                            ).andReturn()
                            
                            if (result.response.status == 200) {
                                likeCount++
                            }
                        } catch (Exception e) {
                            println "Error liking post: ${e.message}"
                        }
                    }
                }
            }
        }
        
        then: 'Like operations succeed'
        likeCount > 0
        
        when: 'A user checks their feed'
        def feedResponse = null
        try {
            def result = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/feed")
                    .header('Authorization', "Bearer ${userTokens[testUsers[0].username]}")
            ).andReturn()
            
            if (result.response.status == 200) {
                feedResponse = objectMapper.readValue(result.response.contentAsString, Map)
            }
        } catch (Exception e) {
            println "Error getting feed: ${e.message}"
        }
        
        then: 'Feed is retrieved successfully'
        feedResponse != null
    }

}
