package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    def setup() {
        // Clear the database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()
        
        // Create a test user
        def user = TestDataFactory.createUser(null, "integrationuser")
        userRepository.save(user)
    }

    @WithMockUser(username = "integrationuser")
    def "should create and retrieve a post"() {
        given:
        def user = userRepository.findByUsername("integrationuser").get()
        def createPostRequest = [content: "Integration test post"]

        when: "Create a new post"
        def createResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createPostRequest))
        )

        then: "Post is created successfully"
        createResult.andExpect(MockMvcResultMatchers.status().isOk())
        
        def postId = objectMapper.readValue(
            createResult.andReturn().response.contentAsString,
            Map
        ).id
        
        when: "Retrieve the created post"
        def getResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/posts/${postId}")
        )
        
        then: "Post details are returned correctly"
        getResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.content').value("Integration test post"))
            .andExpect(MockMvcResultMatchers.jsonPath('$.userId').value(user.id))
    }

    @WithMockUser(username = "integrationuser")
    def "should like and unlike a post"() {
        given:
        def user = userRepository.findByUsername("integrationuser").get()
        def post = TestDataFactory.createPost(null, "Post to like", user.id, user.username)
        postRepository.save(post)

        when: "User likes the post"
        def likeResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/posts/${post.id}/like")
        )

        then: "Like operation succeeds"
        likeResult.andExpect(MockMvcResultMatchers.status().isOk())
        
        when: "Retrieve the post"
        def getResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/posts/${post.id}")
        )
        
        then: "Post shows as liked"
        getResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.likes').isArray())
            .andExpect(MockMvcResultMatchers.jsonPath('$.likes[0]').value(user.id))
        
        when: "User unlikes the post"
        def unlikeResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/posts/${post.id}/like")
        )
        
        then: "Unlike operation succeeds"
        unlikeResult.andExpect(MockMvcResultMatchers.status().isOk())
        
        when: "Retrieve the post again"
        def getFinalResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/posts/${post.id}")
        )
        
        then: "Post no longer shows as liked"
        getFinalResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.likes').isEmpty())
    }
}
