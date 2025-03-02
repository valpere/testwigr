package com.example.testwigr.repository

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@DataMongoTest
@ActiveProfiles("test")
class PostRepositoryTest extends Specification {
    
    @Autowired
    PostRepository postRepository
    
    @Autowired
    UserRepository userRepository
    
    def setup() {
        // Clear the database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()
    }
    
    def "should find posts by user ID"() {
        given: "a user and some posts"
        def user = TestDataFactory.createUser()
        userRepository.save(user)
        
        def post1 = TestDataFactory.createPost(null, "Post 1", user.id, user.username)
        def post2 = TestDataFactory.createPost(null, "Post 2", user.id, user.username)
        postRepository.save(post1)
        postRepository.save(post2)
        
        when: "finding posts by user ID"
        def result = postRepository.findByUserId(user.id, PageRequest.of(0, 10))
        
        then: "correct posts are found"
        result.content.size() == 2
        result.content.any { it.content == "Post 1" }
        result.content.any { it.content == "Post 2" }
    }
    
    def "should find posts by multiple user IDs"() {
        given: "multiple users with posts"
        def user1 = TestDataFactory.createUser(null, "user1")
        def user2 = TestDataFactory.createUser(null, "user2")
        userRepository.save(user1)
        userRepository.save(user2)
        
        def post1 = TestDataFactory.createPost(null, "User 1 post", user1.id, user1.username)
        def post2 = TestDataFactory.createPost(null, "User 2 post", user2.id, user2.username)
        postRepository.save(post1)
        postRepository.save(post2)
        
        when: "finding posts by multiple user IDs"
        def result = postRepository.findByUserIdIn([user1.id, user2.id], PageRequest.of(0, 10))
        
        then: "posts from both users are found"
        result.content.size() == 2
        result.content.any { it.userId == user1.id }
        result.content.any { it.userId == user2.id }
    }
}
