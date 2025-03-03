package com.example.testwigr.integration

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
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class SocialInteractionIntegrationTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    PasswordEncoder passwordEncoder

    private User user1
    private User user2
    private Post post

    def setup() {
        // Clear database
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)

        // Create test users
        user1 = TestDataFactory.createUser(null, 'socialuser1')
        user1.password = passwordEncoder.encode('password')
        userRepository.save(user1)

        user2 = TestDataFactory.createUser(null, 'socialuser2')
        user2.password = passwordEncoder.encode('password')
        userRepository.save(user2)

        // Create a test post
        post = TestDataFactory.createPost(null, 'Test social post', user1.id, user1.username)
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
        )

        then: 'Follow operation succeeds'
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
            .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))

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
        )

        then: 'Like operation succeeds'
        likeResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
            .andExpect(MockMvcResultMatchers.jsonPath('$.isLiked').value(true))

        and: 'Post shows as liked in database'
        def updatedPost = postRepository.findById(post.id).get()
        updatedPost.likes.contains(user2.id)

        when: 'User2 unlikes the post'
        def unlikeResult = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/likes/posts/${post.id}")
        )

        then: 'Unlike operation succeeds'
        unlikeResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
            .andExpect(MockMvcResultMatchers.jsonPath('$.isLiked').value(false))

        and: 'Post no longer shows as liked in database'
        def finalPost = postRepository.findById(post.id).get()
        !finalPost.likes.contains(user2.id)
    }

    @WithMockUser(username = 'socialuser2')
    def "should allow commenting on posts"() {
        given: 'Comment data'
        def commentRequest = [content: 'This is a test comment']

        when: "User2 comments on User1's post"
        def commentResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentRequest))
        )

        then: 'Comment is added successfully'
        commentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: 'Retrieving comments'
        def getCommentsResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/comments/posts/${post.id}")
        )

        then: 'Comment is visible'
        getCommentsResult.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].content').value('This is a test comment'))
            .andExpect(MockMvcResultMatchers.jsonPath('$[0].userId').value(user2.id))
    }

}
