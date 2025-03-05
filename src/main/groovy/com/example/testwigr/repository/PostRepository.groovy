package com.example.testwigr.repository

import com.example.testwigr.model.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PostRepository extends MongoRepository<Post, String> {
    // Find posts by user ID with projection to exclude comments for feed queries
    @Query(value = "{ 'userId': ?0 }", fields = "{ 'comments': 0 }")
    Page<Post> findByUserIdWithoutComments(String userId, Pageable pageable)

    // Find posts from multiple users with projection (for feed generation)
    @Query(value = "{ 'userId': { '\$in': ?0 } }", fields = "{ 'comments': 0 }")
    Page<Post> findByUserIdInWithoutComments(Collection<String> userIds, Pageable pageable)

    // Count posts by user ID (for profile statistics)
    long countByUserId(String userId)

    // Find posts with specific criteria for trending/popular posts
    @Query("{ 'likes': { '\$exists': true, '\$ne': [] } }")
    Page<Post> findPopularPosts(Pageable pageable)

    // Original methods
    Page<Post> findByUserId(String userId, Pageable pageable)
    Page<Post> findByUserIdIn(Collection<String> userIds, Pageable pageable)
}