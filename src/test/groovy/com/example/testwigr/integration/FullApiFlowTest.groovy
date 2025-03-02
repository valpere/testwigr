package com.example.testwigr.integration

import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class FullApiFlowTest extends Specification {
    
    @Autowired
    WebTestClient webTestClient
    
    @Autowired
    UserRepository userRepository
    
    @Autowired
    PostRepository postRepository
    
    @Autowired
    PasswordEncoder passwordEncoder
    
    @Autowired
    ObjectMapper objectMapper
    
    private String authToken
    
    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
        
        // Create a test user
        def user = TestDataFactory.createUser(null, "flowuser")
        user.password = passwordEncoder.encode("flowpassword")
        userRepository.save(user)
        
        // Login to get auth token
        def loginRequest = [
            username: "flowuser",
            password: "flowpassword"
        ]
        
        def loginResponse = webTestClient
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(loginRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .returnResult()
            .responseBody
            
        authToken = loginResponse.token
    }
    
    def "should complete the entire post and social interaction flow"() {
        // Step 1: Create a post
        def postContent = "This is a test post for the full API flow"
        def createPostRequest = [content: postContent]
        
        def postResponse = webTestClient
            .post()
            .uri("/api/posts")
            .header("Authorization", "Bearer ${authToken}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(createPostRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .returnResult()
            .responseBody
            
        def postId = postResponse.id
        
        // Step 2: Retrieve the post
        webTestClient
            .get()
            .uri("/api/posts/${postId}")
            .header("Authorization", "Bearer ${authToken}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath('$.content').isEqualTo(postContent)
            .jsonPath('$.id').isEqualTo(postId)
        
        // Step 3: Add a comment to the post
        def commentContent = "This is a test comment"
        def commentRequest = [content: commentContent]
        
        webTestClient
            .post()
            .uri("/api/comments/posts/${postId}")
            .header("Authorization", "Bearer ${authToken}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(commentRequest))
            .exchange()
            .expectStatus().isOk()
        
        // Step 4: Verify comment was added
        webTestClient
            .get()
            .uri("/api/comments/posts/${postId}")
            .header("Authorization", "Bearer ${authToken}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath('$[0].content').isEqualTo(commentContent)
        
        // Step 5: Like the post
        webTestClient
            .post()
            .uri("/api/posts/${postId}/like")
            .header("Authorization", "Bearer ${authToken}")
            .exchange()
            .expectStatus().isOk()
        
        // Step 6: Verify post is liked
        webTestClient
            .get()
            .uri("/api/posts/${postId}")
            .header("Authorization", "Bearer ${authToken}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath('$.likes[0]').exists()
        
        // Step 7: Unlike the post
        webTestClient
            .delete()
            .uri("/api/posts/${postId}/like")
            .header("Authorization", "Bearer ${authToken}")
            .exchange()
            .expectStatus().isOk()
        
        // Step 8: Verify post is no longer liked
        webTestClient
            .get()
            .uri("/api/posts/${postId}")
            .header("Authorization", "Bearer ${authToken}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath('$.likes').isEmpty()
    }
}
