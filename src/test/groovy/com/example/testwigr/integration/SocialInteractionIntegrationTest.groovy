package com.example.testwigr.integration

import com.example.testwigr.config.TestSecurityConfig
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestDatabaseUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
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
class SocialInteractionIntegrationTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    ObjectMapper objectMapper

    private User user1
    private User user2
    private Post post

    def setup() {
        // Clear database
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)

        // Reset TestDataFactory IDs
        TestDataFactory.resetIds()

        // Create test users with fixed IDs - use predictable IDs
        user1 = TestDataFactory.createUser("user1-id", 'socialuser1')
        userRepository.save(user1)

        user2 = TestDataFactory.createUser("user2-id", 'socialuser2')
        userRepository.save(user2)

        // Create a test post with fixed ID
        post = TestDataFactory.createPost("post-id", 'Test social post', user1.id, user1.username)
        postRepository.save(post)
    }

    def cleanup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    @WithMockUser(username = 'socialuser2')
    def "should allow user to follow another user"() {
        when: 'User2 follows User1'
        def followResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${user1.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser2"))
        )

        then: 'Follow operation succeeds'
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))

        and: 'Database reflects the follow relationship'
        def updatedUser2 = userRepository.findById(user2.id).get()
        updatedUser2.following.contains(user1.id)

        def updatedUser1 = userRepository.findById(user1.id).get()
        updatedUser1.followers.contains(user2.id)
    }

    @WithMockUser(username = 'socialuser2')
    def "should allow user to like and unlike a post"() {
        when: "User2 likes User1's post"
        def likeResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/likes/posts/${post.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser2"))
        )

        then: 'Like operation succeeds'
        likeResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.isLiked').value(true))

        and: 'Post is updated in database'
        def updatedPost = postRepository.findById(post.id).get()
        !updatedPost.likes.isEmpty()

        when: 'User2 unlikes the post'
        def unlikeResult = mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/likes/posts/${post.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser2"))
        )

        then: 'Unlike operation succeeds'
        unlikeResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.isLiked').value(false))

        and: 'Post no longer shows as liked in database'
        def finalPost = postRepository.findById(post.id).get()
        finalPost.likes.isEmpty()
    }

    @WithMockUser(username = 'socialuser2')
    def "should allow commenting on posts"() {
        given: 'Comment data'
        def commentRequest = [content: 'This is a test comment']

        when: "User2 comments on User1's post"
        def commentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest))
        )

        then: 'Comment is added successfully'
        commentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Retrieving comments'
        def getCommentsResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/comments/posts/${post.id}")
                        .with(SecurityMockMvcRequestPostProcessors.user("socialuser2"))
        )

        then: 'Comment is visible'
        getCommentsResult.andExpect(MockMvcResultMatchers.status().isOk())

        // Extract comment response and validate content
        def commentsResponse = objectMapper.readValue(
                getCommentsResult.andReturn().response.contentAsString,
                List
        )
        commentsResponse.size() > 0
        commentsResponse[0].content == 'This is a test comment'
    }
}
