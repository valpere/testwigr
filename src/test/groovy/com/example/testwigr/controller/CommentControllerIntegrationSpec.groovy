package com.example.testwigr.controller

import com.example.testwigr.model.Post
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class CommentControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user
        def user = TestDataFactory.createUser(null, 'commentuser')
        userRepository.save(user)

        // Create a test post
        def user2 = TestDataFactory.createUser(null, 'posteruser')
        userRepository.save(user2)

        def post = TestDataFactory.createPost(null, 'Post for comments', user2.id, user2.username)
        postRepository.save(post)
    }

    @WithMockUser(username = 'commentuser')
    def "should add and retrieve comments on a post"() {
        given:
        def user = userRepository.findByUsername('commentuser').get()
        def post = postRepository.findAll().first()
        def commentRequest = [content: 'This is a test comment']

        println "Test user: ${user.id}, ${user.username}"
        println "Test post: ${post.id}, ${post.content}"

        when: 'Add a comment to the post'
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentRequest))
        ).andReturn()

        // Add detailed error debugging
        if (result.getResponse().getStatus() != 200) {
            println "Error response: ${result.getResponse().getContentAsString()}"
            println "Status code: ${result.getResponse().getStatus()}"
        }

        def commentResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentRequest))
        )

        then: 'Comment is added successfully'
        commentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Retrieve comments for the post'
        def getResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/comments/posts/${post.id}")
        )

        then: 'Comment list contains the added comment'
        getResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].content').value('This is a test comment'))
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].userId').value(user.id))
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].username').value('commentuser'))
    }

}
