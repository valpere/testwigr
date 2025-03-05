package com.example.testwigr.controller

import com.example.testwigr.model.Comment
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.PostService
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@RestController
@RequestMapping('/api/comments')
@Tag(name = 'Comments', description = 'Comment management API')
class CommentController {

    private final PostService postService
    private final UserService userService

    CommentController(PostService postService, UserService userService) {
        this.postService = postService
        this.userService = userService
    }

    @PostMapping('/posts/{postId}')
    @Operation(summary = 'Add a comment to a post')
    ResponseEntity<Post> addComment(
            @PathVariable("postId") String postId,
            @Valid @RequestBody CreateCommentRequest createCommentRequest,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal()
            User user = userService.getUserByUsername(userDetails.getUsername())

            Post post = postService.addComment(postId, createCommentRequest.content, user.id)
            return ResponseEntity.ok(post)
        } catch (Exception e) {
            e.printStackTrace()
            throw e
        }
    }

    @GetMapping('/posts/{postId}')
    @Operation(summary = 'Get all comments for a post')
    ResponseEntity<List<Comment>> getCommentsByPostId(@PathVariable("postId") String postId) {
        List<Comment> comments = postService.getCommentsByPostId(postId)
        return ResponseEntity.ok(comments)
    }

    // Note: This method is commented out because PostService doesn't have deleteComment implementation yet
    // If you want to implement comment deletion, you'll need to add this method to PostService first
    /*
    @DeleteMapping('/posts/{postId}/{commentId}')
    @Operation(summary = 'Delete a comment from a post')
    ResponseEntity<?> deleteComment(
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        // Implement comment deletion logic in your PostService
        postService.deleteComment(postId, commentId, user.id)
        return ResponseEntity.ok([
            success: true,
            message: 'Comment deleted successfully'
        ])
    }
    */

    static class CreateCommentRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        String content
    }
}
