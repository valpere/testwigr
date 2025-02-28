package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.model.Comment
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class PostService {

    private final PostRepository postRepository
    private final UserService userService

    PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository
        this.userService = userService
    }

    Post createPost(String content, String userId) {
        User user = userService.getUserById(userId)
        Post post = new Post(content, userId, user.username)
        return postRepository.save(post)
    }

    Post getPostById(String id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException('Post not found with id: ' + id))
    }

    Page<Post> getPostsByUserId(String userId, Pageable pageable) {
        // Verify user exists
        userService.getUserById(userId)
        return postRepository.findByUserId(userId, pageable)
    }

    Page<Post> getFeedForUser(String userId, Pageable pageable) {
        User user = userService.getUserById(userId)

        // Get posts from users they follow, plus their own posts
        Set<String> followingIds = new HashSet<>(user.following)
        followingIds.add(userId) // Add user's own posts to feed

        return postRepository.findByUserIdIn(followingIds, pageable)
    }

    Post updatePost(String id, String content, String userId) {
        Post post = getPostById(id)

        // Check if the user is the author of the post
        if (!post.userId.equals(userId)) {
            throw new SecurityException('You can only update your own posts')
        }

        post.content = content
        post.updatedAt = LocalDateTime.now()

        return postRepository.save(post)
    }

    void deletePost(String id, String userId) {
        Post post = getPostById(id)

        // Check if the user is the author of the post
        if (!post.userId.equals(userId)) {
            throw new SecurityException('You can only delete your own posts')
        }

        postRepository.delete(post)
    }

    Post likePost(String postId, String userId) {
        Post post = getPostById(postId)
        post.likes.add(userId)
        return postRepository.save(post)
    }

    Post unlikePost(String postId, String userId) {
        Post post = getPostById(postId)
        post.likes.remove(userId)
        return postRepository.save(post)
    }

    Post addComment(String postId, String content, String userId) {
        Post post = getPostById(postId)
        User user = userService.getUserById(userId)

        Comment comment = new Comment(content, userId, user.username)
        post.comments.add(comment)

        return postRepository.save(post)
    }

    List<Comment> getCommentsByPostId(String postId) {
        Post post = getPostById(postId)
        return post.comments
    }

    Page<Post> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
    }

}
