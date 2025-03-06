package com.example.testwigr.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Represents a user post in the system.
 * A post is a content entry created by a user that can receive likes and comments.
 */
@Document(collection = "posts")
@Schema(description = "Represents a user post with content, likes, and comments")
class Post {
    @Id
    @Schema(description = "Unique identifier for the post", example = "60d21b4667d1d12d98a8e543")
    String id

    @Schema(description = "Text content of the post", example = "This is my first post on Testwigr!", required = true)
    String content

    @Schema(description = "ID of the user who created the post", example = "60d21b4667d1d12d98a8e123", required = true)
    String userId

    @Schema(description = "Username of the user who created the post", example = "johndoe", required = true)
    String username

    @ArraySchema(schema = @Schema(description = "ID of a user who liked the post", example = "60d21b4667d1d12d98a8e456"))
    @Schema(description = "Set of user IDs who liked the post")
    Set<String> likes = []

    @ArraySchema(schema = @Schema(implementation = Comment))
    @Schema(description = "List of comments on the post")
    List<Comment> comments = []

    @Schema(description = "Date and time when the post was created", example = "2023-01-15T14:30:15.123")
    LocalDateTime createdAt

    @Schema(description = "Date and time when the post was last updated", example = "2023-01-15T15:45:22.456")
    LocalDateTime updatedAt

    /**
     * Default constructor required by MongoDB.
     */
    Post() {}

    /**
     * Creates a new post with the specified content, user ID, and username.
     *
     * @param content The text content of the post
     * @param userId The ID of the user creating the post
     * @param username The username of the user creating the post
     */
    Post(String content, String userId, String username) {
        this.content = content
        this.userId = userId
        this.username = username
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Checks if a specific user has liked this post.
     *
     * @param userId The ID of the user to check
     * @return true if the user has liked the post, false otherwise
     */
    @JsonIgnore
    @Schema(hidden = true)
    boolean isLikedBy(String userId) {
        return likes.contains(userId)
    }

    /**
     * Gets the total number of likes on this post.
     *
     * @return The number of likes
     */
    @Schema(description = "Total number of likes on the post", example = "42")
    int getLikeCount() {
        return likes.size()
    }

    /**
     * Gets the total number of comments on this post.
     *
     * @return The number of comments
     */
    @Schema(description = "Total number of comments on the post", example = "7")
    int getCommentCount() {
        return comments.size()
    }

}
