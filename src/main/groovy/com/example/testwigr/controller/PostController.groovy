package com.example.testwigr.controller

import com.example.testwigr.model.Comment
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.PostService
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@RestController
@RequestMapping('/api/posts')
@Tag(name = 'Posts', description = 'Post management API')
class PostController {

    private final PostService postService
    private final UserService userService

    PostController(PostService postService, UserService userService) {
        this.postService = postService
        this.userService = userService
    }

    @PostMapping
    @Operation(summary = 'Create a new post')
    ResponseEntity<Post> createPost(@Valid @RequestBody CreatePostRequest createPostRequest, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.createPost(createPostRequest.content, user.id)
        return ResponseEntity.ok(post)
    }

    @GetMapping('/{id}')
    @Operation(summary = 'Get a post by ID')
    ResponseEntity<Post> getPostById(@PathVariable("id") String id) {
        Post post = postService.getPostById(id)
        return ResponseEntity.ok(post)
    }

    @GetMapping('/user/{userId}')
    ResponseEntity<Page<Post>> getPostsByUserId(
            @PathVariable("userId") String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Post> posts = postService.getPostsByUserId(userId, pageable)
        return ResponseEntity.ok(posts)
    }

    @GetMapping('/feed')
    ResponseEntity<Page<Post>> getFeed(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Page<Post> feed = postService.getFeedForUser(user.id, pageable)
        return ResponseEntity.ok(feed)
    }

    @PutMapping('/{id}')
    @Operation(summary = 'Update a post')
    ResponseEntity<Post> updatePost(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdatePostRequest updatePostRequest,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post updatedPost = postService.updatePost(id, updatePostRequest.content, user.id)
        return ResponseEntity.ok(updatedPost)
    }

    @DeleteMapping('/{id}')
    @Operation(summary = 'Delete a post')
    ResponseEntity<?> deletePost(@PathVariable("id") String id, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        postService.deletePost(id, user.id)
        return ResponseEntity.ok([
            success: true,
            message: 'Post deleted successfully'
        ])
    }

    @PostMapping('/{id}/like')
    ResponseEntity<Post> likePost(@PathVariable("id") String id, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.likePost(id, user.id)
        return ResponseEntity.ok(post)
    }

    @DeleteMapping('/{id}/like')
    ResponseEntity<Post> unlikePost(@PathVariable("id") String id, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.unlikePost(id, user.id)
        return ResponseEntity.ok(post)
    }

    @PostMapping('/{id}/comments')
    ResponseEntity<Post> addComment(
            @PathVariable("id") String id,
            @Valid @RequestBody CommentRequest commentRequest,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.addComment(id, commentRequest.content, user.id)
        return ResponseEntity.ok(post)
    }

    @GetMapping('/{id}/comments')
    ResponseEntity<List<Comment>> getComments(@PathVariable("id") String id) {
        List<Comment> comments = postService.getCommentsByPostId(id)
        return ResponseEntity.ok(comments)
    }

    static class CreatePostRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        String content
    }

    static class UpdatePostRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        String content
    }

    static class CommentRequest {
        String content
    }

}
