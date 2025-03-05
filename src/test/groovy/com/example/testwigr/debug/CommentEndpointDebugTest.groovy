package com.example.testwigr.debug

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
@Import(TestSecurityConfig)
class CommentEndpointDebugTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    def setup() {
        // Clear database
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create admin user
        User admin = TestDataFactory.createUser(null, 'admin')
        userRepository.save(admin)

        // Create a test post
        def user = TestDataFactory.createUser(null, 'testuser')
        userRepository.save(user)

        def post = TestDataFactory.createPost("test-post-id", "Test post content", user.id, user.username)
        postRepository.save(post)
    }

    @WithMockUser(username = "admin")
    def "should debug comment endpoint"() {
        given:
        def simpleContent = '{"content":"Test comment"}'

        when:
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/test-post-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simpleContent)
        )
                .andDo(MockMvcResultHandlers.print())
                .andReturn()

        then:
        println "Status: ${result.response.status}"
        println "Response: ${result.response.contentAsString}"
        if (result.resolvedException) {
            println "Exception: ${result.resolvedException.class.name}"
            println "Message: ${result.resolvedException.message}"
        }

        true // Just for the test to pass
    }
}
