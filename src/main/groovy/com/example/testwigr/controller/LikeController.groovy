package com.example.testwigr.controller

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.PostService
import com.example.testwigr.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping('/api/likes')
class LikeController {

    private final PostService postService
    private final UserService userService

    LikeController(PostService postService, UserService userService) {
        this.postService = postService
        this.userService = userService
    }

    @PostMapping('/posts/{postId}')
    ResponseEntity<Map<String, Object>> likePost(
            @PathVariable('postId') String postId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.likePost(postId, user.id)

        return ResponseEntity.ok([
            success: true,
            likeCount: post.getLikeCount(),
            isLiked: true
        ])
    }

    @DeleteMapping('/posts/{postId}')
    ResponseEntity<Map<String, Object>> unlikePost(
            @PathVariable('postId') String postId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.unlikePost(postId, user.id)

        return ResponseEntity.ok([
            success: true,
            likeCount: post.getLikeCount(),
            isLiked: false
        ])
    }

    @GetMapping('/posts/{postId}')
    ResponseEntity<Map<String, Object>> getLikeStatus(
            @PathVariable('postId') String postId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.getPostById(postId)

        return ResponseEntity.ok([
            likeCount: post.getLikeCount(),
            isLiked: post.isLikedBy(user.id)
        ])
    }

    @GetMapping('/posts/{postId}/users')
    ResponseEntity<List<User>> getLikedUsers(@PathVariable('postId') String postId) {
        Post post = postService.getPostById(postId)

        List<User> likedUsers = []
        for (String userId : post.likes) {
            try {
                User likedUser = userService.getUserById(userId)
                likedUsers.add(likedUser)
            } catch (ResourceNotFoundException e) {
                // Skip users that might have been deleted
            }
        }

        return ResponseEntity.ok(likedUsers)
    }

}
