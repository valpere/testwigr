package com.example.testwigr.repository

import com.example.testwigr.config.MongoIntegrationSpec
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class PostRepositoryIntegrationSpec extends MongoIntegrationSpec {

    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    def cleanup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "should save and find posts"() {
        given:
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        def post1 = TestDataFactory.createPost(null, 'Test post 1', user.id, user.username)
        def post2 = TestDataFactory.createPost(null, 'Test post 2', user.id, user.username)

        when:
        postRepository.save(post1)
        postRepository.save(post2)
        def posts = postRepository.findByUserId(user.id, PageRequest.of(0, 10))

        then:
        posts.content.size() == 2
        posts.content.any { it.content == 'Test post 1' }
        posts.content.any { it.content == 'Test post 2' }
    }

    def "should find posts by multiple user ids"() {
        given:
        def user1 = TestDataFactory.createUser(null, 'user1')
        def user2 = TestDataFactory.createUser(null, 'user2')
        userRepository.save(user1)
        userRepository.save(user2)

        def post1 = TestDataFactory.createPost(null, 'Post by user1', user1.id, user1.username)
        def post2 = TestDataFactory.createPost(null, 'Post by user2', user2.id, user2.username)
        postRepository.save(post1)
        postRepository.save(post2)

        when:
        def posts = postRepository.findByUserIdIn([user1.id, user2.id], PageRequest.of(0, 10))

        then:
        posts.content.size() == 2
        posts.content.any { it.userId == user1.id }
        posts.content.any { it.userId == user2.id }
    }

}