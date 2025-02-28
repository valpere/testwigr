package com.example.testwigr.controller

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.PostService
import com.example.testwigr.service.UserService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Specification

class PostControllerSpec extends Specification {
    
    PostService postService
    UserService userService
    PostController postController
    
    def setup() {
        postService = Mock(PostService)
        userService = Mock(UserService)
        postController = new PostController(postService, userService)
    }
    
    def "should create a post"() {
        given:
        def postRequest = new PostController.PostRequest(content: "Test post")
        def userId = "123"
        def user = new User(id: userId, username: "testuser")
        def post = new Post(content: "Test post", userId: userId, username: "testuser")
        
        and:
        def authentication = Mock(Authentication)
        def userDetails = Mock(UserDetails)
        authentication.getPrincipal() >> userDetails
        userDetails.getUsername() >> "testuser"
        userService.getUserByUsername("testuser") >> user
        postService.createPost("Test post", userId) >> post
        
        when:
        def response = postController.createPost(postRequest, authentication)
        
        then:
        response.body.content == "Test post"
        response.body.userId == userId
    }
    
    def "should get post by id"() {
        given:
        def postId = "456"
        def post = new Post(id: postId, content: "Test post")
        
        and:
        postService.getPostById(postId) >> post
        
        when:
        def response = postController.getPostById(postId)
        
        then:
        response.body.id == postId
        response.body.content == "Test post"
    }
    
    def "should get posts by user id"() {
        given:
        def userId = "123"
        def posts = [new Post(content: "Post 1"), new Post(content: "Post 2")]
        def pageable = Mock(Pageable)
        
        and:
        postService.getPostsByUserId(userId, pageable) >> new PageImpl<>(posts)
        
        when:
        def response = postController.getPostsByUserId(userId, pageable)
        
        then:
        response.body.content.size() == 2
    }
    
    def "should like a post"() {
        given:
        def postId = "456"
        def userId = "123"
        def user = new User(id: userId, username: "testuser")
        def post = new Post(id: postId, content: "Test post", likes: [userId] as Set)
        
        and:
        def authentication = Mock(Authentication)
        def userDetails = Mock(UserDetails)
        authentication.getPrincipal() >> userDetails
        userDetails.getUsername() >> "testuser"
        userService.getUserByUsername("testuser") >> user
        postService.likePost(postId, userId) >> post
        
        when:
        def response = postController.likePost(postId, authentication)
        
        then:
        response.body.likes.contains(userId)
    }
}
