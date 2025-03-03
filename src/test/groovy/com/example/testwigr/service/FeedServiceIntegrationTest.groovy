package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestDatabaseUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
class FeedServiceIntegrationTest extends Specification {

    @Autowired
    FeedService feedService

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    def setup() {
        // Clear database first
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)

        // Create a social network for testing
        TestDatabaseUtils.createSocialNetwork(userRepository, passwordEncoder, 5)

        // Create posts for each user
        userRepository.findAll().each { user ->
            3.times { i ->
                def post = TestDataFactory.createPost(null, "Feed post ${i} from ${user.username}", user.id, user.username)
                postRepository.save(post)
            }
        }
    }

    def cleanup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    def "should get personal feed containing posts from followed users"() {
        given:
        def testUser = userRepository.findAll().first()
        def pageable = PageRequest.of(0, 20)

        when:
        def result = feedService.getPersonalFeed(testUser.id, pageable)

        then:
        // Should contain posts from user and followed users (3 users * 3 posts each = 9 posts)
        result.content.size() == 9

        // Should contain posts from self
        result.content.findAll { it.userId == testUser.id }.size() == 3

        // Should contain posts from followed users
        def followedIds = testUser.following
        result.content.findAll { followedIds.contains(it.userId) }.size() == 6
    }

    def "should get user feed containing only that user's posts"() {
        given:
        def testUser = userRepository.findAll().first()
        def pageable = PageRequest.of(0, 20)

        when:
        def result = feedService.getUserFeed(testUser.id, pageable)

        then:
        // Should only contain posts from the target user
        result.content.size() == 3
        result.content.every { it.userId == testUser.id }
    }

}

