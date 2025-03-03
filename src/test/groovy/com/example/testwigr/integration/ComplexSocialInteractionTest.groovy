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
        // Create users and login to get tokens
        userCount.times { i ->
            def username = "socialuser${i}"
            def password = "password123"

            // Create and save user
            def user = TestDataFactory.createUser(null, username)
            user.password = passwordEncoder.encode(password)
            def savedUser = userRepository.save(user)
            testUsers << savedUser

            // Login to get token
            def loginRequest = [
                username: username,
                password: password
            ]

            def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn()

            def loginResponse = objectMapper.readValue(
                loginResult.response.contentAsString,
                Map
            )

            userTokens[username] = loginResponse.token
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
            def followerUsername = "socialuser${followerIdx}"
            def followerToken = userTokens[followerUsername]

            followeeIdxs.each { followeeIdx ->
                def followeeId = testUsers[followeeIdx].id

                def followResult = mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/follow/${followeeId}")
                        .header('Authorization', "Bearer ${followerToken}")
                )

                if (followResult.andReturn().response.status == 200) {
                    successfulFollows++
                }
            }
        }

        then: 'Follow operations complete successfully'
        successfulFollows == 12 // Total number of follows in our pattern

        // 2. Each user creates some posts
        when: 'Users create posts'
        def posts = []

        testUsers.eachWithIndex { user, idx ->
            def username = user.username
            def token = userTokens[username]

            3.times { postIdx ->
                def postContent = "Post ${postIdx} from ${username}"
                def createPostRequest = [content: postContent]

                def createPostResult = mockMvc.perform(
                    MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest))
                ).andReturn()

                def postResponse = objectMapper.readValue(
                    createPostResult.response.contentAsString,
                    Map
                )

                posts << [
                    id: postResponse.id,
                    content: postContent,
                    authorIdx: idx
                ]
            }
        }

        then: 'Posts are created successfully'
        posts.size() == 15 // 5 users x 3 posts each

        // 3. Users like and comment on various posts
        when: 'Users interact with posts'
        def interactions = 0

        // Create a specific interaction pattern
        // Each user likes 5 random posts and comments on 3 random posts
        testUsers.eachWithIndex { user, userIdx ->
            def username = user.username
            def token = userTokens[username]

            // Get 5 random posts to like
            def shuffledPosts = new ArrayList<>(posts)
            Collections.shuffle(shuffledPosts)
            def postsToLike = shuffledPosts.subList(0, 5)

            // Like the selected posts
            postsToLike.each { post ->
                mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/likes/posts/${post.id}")
                        .header('Authorization', "Bearer ${token}")
                )
                interactions++
            }

            // Get 3 random posts to comment on
            def postsToComment = shuffledPosts.subList(5, 8)

            // Comment on the selected posts
            postsToComment.each { post ->
                def commentRequest = [
                    content: "Comment from ${username} on post by user${post.authorIdx}"
                ]

                mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest))
                )
                interactions++
            }
        }

        then: 'Social interactions complete successfully'
        interactions == 40 // 5 users x (5 likes + 3 comments)

        // 4. Test feed generation
        when: 'Users check their feeds'
        def user0Feed = mockMvc.perform(
            MockMvcRequestBuilders.get('/api/feed')
                .header('Authorization', "Bearer ${userTokens['socialuser0']}")
        )

        then: 'Feed contains expected posts'
        // User 0 follows users 1, 2, plus own posts
        // So feed should have at least 9 posts (3 own + 3 from user1 + 3 from user2)
        user0Feed.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content').isArray())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content.length()').value(9))

        when: 'User 4 checks their feed'
        def user4Feed = mockMvc.perform(
            MockMvcRequestBuilders.get('/api/feed')
                .header('Authorization', "Bearer ${userTokens['socialuser4']}")
        )

        then: 'Feed contains all posts from followed users'
        // User 4 follows users 0, 1, 2, 3, plus own posts
        // So feed should have 15 posts (3 own + 3 from user0 + 3 from user1 + 3 from user2 + 3 from user3)
        user4Feed.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content').isArray())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content.length()').value(15))

        // 5. Test user follow status
        when: 'User 0 checks follow status with User 1'
        def user0Status = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/follow/${testUsers[1].id}/status")
                .header('Authorization', "Bearer ${userTokens['socialuser0']}")
        )

        then: 'Status shows correct follow relationship'
        user0Status.andExpect(MockMvcResultMatchers.status().isOk())
                  .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))
                  .andExpect(MockMvcResultMatchers.jsonPath('$.isFollower').value(true))

        when: 'User 0 checks follow status with User 4'
        def user0Status2 = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/follow/${testUsers[4].id}/status")
                .header('Authorization', "Bearer ${userTokens['socialuser0']}")
        )

        then: 'Status shows correct one-way follow relationship'
        user0Status2.andExpect(MockMvcResultMatchers.status().isOk())
                   .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(false))
                   .andExpect(MockMvcResultMatchers.jsonPath('$.isFollower').value(true))
    }
}
