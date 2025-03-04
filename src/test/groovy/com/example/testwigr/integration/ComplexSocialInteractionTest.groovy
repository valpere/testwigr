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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class ComplexSocialInteractionTest extends Specification {

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

        // Create a network of 5 users
        createTestUserNetwork(5)
    }

    def cleanup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    private void createTestUserNetwork(int userCount) {
        // Create users and get tokens directly with TestSecurityUtils
        userCount.times { i ->
            String username = 'socialuser' + i

            // Create and save user
            def user = TestDataFactory.createUser(null, username)
            user.password = passwordEncoder.encode('password123')
            def savedUser = userRepository.save(user)
            testUsers << savedUser

            // Generate token directly using TestSecurityUtils (avoid login endpoint)
            userTokens[username] = TestSecurityUtils.generateTestToken(username, jwtSecret)
        }
    }

    def "should test complex social network interactions"() {
        // 1. Each user follows some other users
        when: 'Users follow each other in a complex pattern'
        def successfulFollows = 0

        // User 0 follows users 1, 2
        // User 1 follows users 0, 2, 3
        // User 2 follows users 0, 4
        // User 3 follows users 1, 4
        // User 4 follows users 0, 1, 2, 3

        def followPattern = [
            0: [1, 2],
            1: [0, 2, 3],
            2: [0, 4],
            3: [1, 4],
            4: [0, 1, 2, 3]
        ]

        followPattern.each { followerIdx, followeeIdxs ->
            String followerUsername = 'socialuser' + followerIdx
            def followerToken = userTokens[followerUsername]

            if (followerToken) {
                followeeIdxs.each { followeeIdx ->
                    if (followeeIdx < testUsers.size()) {
                        def followeeId = testUsers[followeeIdx].id

                        try {
                            def followResult = mockMvc.perform(
                                MockMvcRequestBuilders.post("/api/follow/${followeeId}")
                                    .header('Authorization', 'Bearer ' + followerToken)
                            ).andReturn()

                            if (followResult.response.status == 200) {
                                successfulFollows++
                            }
                        } catch (Exception e) {
                            println "Error following user ${followeeIdx} by user ${followerIdx}: ${e.message}"
                        }
                    }
                }
            }
        }

        then: 'Follow operations complete successfully'
        successfulFollows > 0

        // 2. Each user creates some posts
        when: 'Users create posts'
        def posts = []

        testUsers.eachWithIndex { user, idx ->
            String username = user.username
            def token = userTokens[username]

            if (token) {
                (1..3).each { postIdx ->
                    // Convert GString to Java String
                    String postContent = 'Post ' + postIdx + ' from ' + username
                    Map<String, String> createPostRequest = [content: postContent]

                    try {
                        def createPostResult = mockMvc.perform(
                            MockMvcRequestBuilders.post('/api/posts')
                                .header('Authorization', 'Bearer ' + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createPostRequest))
                        ).andReturn()

                        if (createPostResult.response.status == 200) {
                            def postResponse = objectMapper.readValue(
                                createPostResult.response.contentAsString,
                                Map
                            )

                            posts << [
                                id: postResponse.id,
                                content: postContent,
                                authorIdx: idx
                            ]
                        } else {
                            println "Failed to create post. Status: ${createPostResult.response.status}"
                            println "Response body: ${createPostResult.response.contentAsString}"
                        }
                    } catch (Exception e) {
                        println "Error creating post for user ${username}: ${e.message}"
                    }
                }
            }
        }

        then: 'Posts are created successfully'
        !posts.isEmpty()

        // 3. Users like and comment on various posts
        when: 'Users interact with posts'
        def interactions = 0

        // Only proceed if we have posts to interact with
        if (!posts.isEmpty()) {
            // Create a specific interaction pattern
            // Each user likes and comments on available posts
            testUsers.eachWithIndex { user, userIdx ->
                String username = user.username
                def token = userTokens[username]

                if (token) {
                    // Get some posts to like (up to 5 or as many as available)
                    def postsToLike = posts.take(Math.min(5, posts.size()))

                    // Like the selected posts
                    postsToLike.each { post ->
                        try {
                            def likeResult = mockMvc.perform(
                                MockMvcRequestBuilders.post("/api/likes/posts/${post.id}")
                                    .header('Authorization', 'Bearer ' + token)
                            ).andReturn()

                            if (likeResult.response.status == 200) {
                                interactions++
                            }
                        } catch (Exception e) {
                            println "Error liking post ${post.id} by user ${username}: ${e.message}"
                        }
                    }

                    // Get some posts to comment on (up to 3 or as many as available)
                    def postsToComment = posts.take(Math.min(3, posts.size()))

                    // Comment on the selected posts
                    postsToComment.each { post ->
                        // Convert GString to Java String
                        String commentContent = 'Comment from ' + username + ' on post by user' + post.authorIdx
                        Map<String, String> commentRequest = [content: commentContent]

                        try {
                            def commentResult = mockMvc.perform(
                                MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                                    .header('Authorization', 'Bearer ' + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(commentRequest))
                            ).andReturn()

                            if (commentResult.response.status == 200) {
                                interactions++
                            }
                        } catch (Exception e) {
                            println "Error commenting on post ${post.id} by user ${username}: ${e.message}"
                        }
                    }
                }
            }
        }

        then: 'Social interactions complete successfully'
        interactions > 0

        // 4. Test feed generation
        when: 'A user checks their feed'
        def feedResult = null
        if (!testUsers.isEmpty() && userTokens.containsKey(testUsers[0].username)) {
            try {
                feedResult = mockMvc.perform(
                    MockMvcRequestBuilders.get('/api/feed')
                        .header('Authorization', 'Bearer ' + userTokens[testUsers[0].username])
                ).andReturn()

                println "Feed status: ${feedResult.response.status}"
                println "Feed response: ${feedResult.response.contentAsString.take(100)}..."
            } catch (Exception e) {
                println "Error getting feed: ${e.message}"
            }
        }

        then: 'Feed request completes successfully'
        feedResult != null && feedResult.response.status == 200
    }

}
