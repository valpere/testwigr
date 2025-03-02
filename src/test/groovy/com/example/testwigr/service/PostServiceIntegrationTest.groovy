package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest(properties = [
    'spring.main.allow-bean-definition-overriding=true',
    'spring.main.allow-circular-references=true'
])
@ActiveProfiles('test')
class PostServiceIntegrationTest extends Specification {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder()
        }
    }

    @Autowired
    PostService postService

    @Autowired
    UserService userService

    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    def setup() {
        // Clear the database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "should create and retrieve a post"() {
        given: 'a user'
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        when: 'creating a post'
        def post = postService.createPost('Test content', user.id)

        then: 'post is created with correct data'
        post.content == 'Test content'
        post.userId == user.id
        post.username == user.username

        when: 'retrieving the post by ID'
        def retrievedPost = postService.getPostById(post.id)

        then: 'the correct post is retrieved'
        retrievedPost.id == post.id
        retrievedPost.content == 'Test content'
    }

    def "should like and unlike a post"() {
        given: 'a user and a post'
        def user = TestDataFactory.createUser()
        userRepository.save(user)
        def post = TestDataFactory.createPost(null, 'Post to like', user.id, user.username)
        postRepository.save(post)

        when: 'liking the post'
        def likedPost = postService.likePost(post.id, user.id)

        then: 'post shows as liked'
        likedPost.likes.contains(user.id)
        likedPost.isLikedBy(user.id)

        when: 'unliking the post'
        def unlikedPost = postService.unlikePost(post.id, user.id)

        then: 'post shows as unliked'
        !unlikedPost.likes.contains(user.id)
        !unlikedPost.isLikedBy(user.id)
    }

}
