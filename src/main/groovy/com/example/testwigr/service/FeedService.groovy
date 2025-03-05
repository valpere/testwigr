package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class FeedService {

    private final PostRepository postRepository
    private final UserService userService

    FeedService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository
        this.userService = userService
    }

    // Get user's personal feed (posts from followed users + own posts)
    @Cacheable("personalFeed")
    Page<Post> getPersonalFeed(String userId, Pageable pageable) {
        User user = userService.getUserById(userId)

        // Get posts from users they follow and their own posts
        Set<String> userIds = new HashSet<>(user.following)
        userIds.add(userId) // Include user's own posts

        // Use optimized query that excludes comments for better performance
        if (postRepository.respondsTo('findByUserIdInWithoutComments')) {
            return postRepository.findByUserIdInWithoutComments(userIds, pageable)
        } else {
            return postRepository.findByUserIdIn(userIds, pageable)
        }
    }

    // Get posts from a specific user
    @Cacheable("userFeed")
    Page<Post> getUserFeed(String targetUserId, Pageable pageable) {
        // Verify user exists
        userService.getUserById(targetUserId)

        // Use optimized query
        if (postRepository.respondsTo('findByUserIdWithoutComments')) {
            return postRepository.findByUserIdWithoutComments(targetUserId, pageable)
        } else {
            return postRepository.findByUserId(targetUserId, pageable)
        }
    }

    // Get popular posts for discovery feed
    @Cacheable("discoveryFeed")
    Page<Post> getDiscoveryFeed(Pageable pageable) {
        if (postRepository.respondsTo('findPopularPosts')) {
            return postRepository.findPopularPosts(pageable)
        } else {
            return postRepository.findAll(pageable)
        }
    }
}
