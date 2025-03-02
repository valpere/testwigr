package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.repository.PostRepository
import spock.lang.Specification

class LikeServiceSpec extends Specification {
    
    PostRepository postRepository

    UserService userService
    PostService postService
    
    def setup() {
        postRepository = Mock(PostRepository)
        userService = Mock(UserService)
        postService = new PostService(postRepository, userService)
    }
    
    def "should like a post"() {
        given:
        def postId = "456"
        def userId = "123"
        def post = new Post(id: postId, content: "Test post", likes: [] as Set)
        
        and:
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_) >> { Post p -> p }
        
        when:
        def result = postService.likePost(postId, userId)
        
        then:
        result.likes.contains(userId)
        result.isLikedBy(userId)
        result.getLikeCount() == 1
    }
    
    def "should unlike a post"() {
        given:
        def postId = "456"
        def userId = "123"
        def post = new Post(id: postId, content: "Test post", likes: [userId] as Set)
        
        and:
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_) >> { Post p -> p }
        
        when:
        def result = postService.unlikePost(postId, userId)
        
        then:
        !result.likes.contains(userId)
        !result.isLikedBy(userId)
        result.getLikeCount() == 0
    }
}
