package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

class PostServiceSpec extends Specification {

    PostRepository postRepository

    UserService userService
    PostService postService

    def setup() {
        postRepository = Mock(PostRepository)
        userService = Mock(UserService)
        postService = new PostService(postRepository, userService)
    }

    def "should create a post successfully"() {
        given:
        def userId = '123'
        def content = 'This is a test post'
        def user = new User(id: userId, username: 'testuser')

        and:
        userService.getUserById(userId) >> user
        postRepository.save(_ as Post) >> { Post post -> post }

        when:
        def result = postService.createPost(content, userId)

        then:
        result.content == content
        result.userId == userId
        result.username == 'testuser'
    }

    def "should get post by id"() {
        given:
        def postId = '456'
        def post = new Post(id: postId, content: 'Test post', userId: '123', username: 'testuser')

        and:
        postRepository.findById(postId) >> Optional.of(post)

        when:
        def result = postService.getPostById(postId)

        then:
        result.id == postId
        result.content == 'Test post'
    }

    def "should throw exception when post id not found"() {
        given:
        def postId = 'nonexistent'

        and:
        postRepository.findById(postId) >> Optional.empty()

        when:
        postService.getPostById(postId)

        then:
        thrown(ResourceNotFoundException)
    }

    def "should get posts by user id"() {
        given:
        def userId = '123'
        def user = new User(id: userId, username: 'testuser')
        def posts = [new Post(content: 'Post 1', userId: userId), new Post(content: 'Post 2', userId: userId)]
        def pageable = PageRequest.of(0, 10)

        and:
        userService.getUserById(userId) >> user
        postRepository.findByUserId(userId, pageable) >> new PageImpl<>(posts)

        when:
        def result = postService.getPostsByUserId(userId, pageable)

        then:
        result.content.size() == 2
        result.content[0].content == 'Post 1'
        result.content[1].content == 'Post 2'
    }

    def "should get feed for user"() {
        given:
        def userId = '123'
        def followingId = '456'
        def user = new User(id: userId, username: 'testuser', following: [followingId] as Set)
        def posts = [
            new Post(content: 'Post 1', userId: userId),
            new Post(content: 'Post 2', userId: followingId)
        ]
        def pageable = PageRequest.of(0, 10)

        and:
        userService.getUserById(userId) >> user
        // Use more flexible argument matcher
        postRepository.findByUserIdIn(_ as Collection, _ as Pageable) >> new PageImpl<>(posts)

        when:
        def result = postService.getFeedForUser(userId, pageable)

        then:
        result.content.size() == 2
    }

    // Additional tests for like, unlike, and comment methods
    def "should like a post"() {
        given:
        def postId = '456'
        def userId = '123'
        def post = new Post(id: postId, content: 'Test post', likes: [] as Set)

        and:
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_ as Post) >> { Post p -> p }

        when:
        def result = postService.likePost(postId, userId)

        then:
        result.likes.contains(userId)
        result.isLikedBy(userId)
    }

    def "should unlike a post"() {
        given:
        def postId = '456'
        def userId = '123'
        def post = new Post(id: postId, content: 'Test post', likes: [userId] as Set)

        and:
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_ as Post) >> { Post p -> p }

        when:
        def result = postService.unlikePost(postId, userId)

        then:
        !result.likes.contains(userId)
        !result.isLikedBy(userId)
    }

    def "should add a comment to a post"() {
        given:
        def postId = '456'
        def userId = '123'
        def content = 'This is a comment'
        def post = new Post(id: postId, content: 'Test post', comments: [])
        def user = new User(id: userId, username: 'testuser')

        and:
        postRepository.findById(postId) >> Optional.of(post)
        userService.getUserById(userId) >> user
        postRepository.save(_ as Post) >> { Post p -> p }

        when:
        def result = postService.addComment(postId, content, userId)

        then:
        result.comments.size() == 1
        result.comments[0].content == content
        result.comments[0].userId == userId
        result.comments[0].username == 'testuser'
    }
}
