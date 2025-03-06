package com.example.testwigr.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Represents a comment on a post.
 * A comment is a text response to a post created by a user.
 */
@Schema(description = "Represents a user comment on a post")
class Comment {
    @Schema(description = "Unique identifier for the comment", example = "60d21b4667d1d12d98a8e789")
    String id

    @Schema(description = "Text content of the comment", example = "Great post! Thanks for sharing.", required = true)
    String content

    @Schema(description = "ID of the user who created the comment", example = "60d21b4667d1d12d98a8e123", required = true)
    String userId

    @Schema(description = "Username of the user who created the comment", example = "johndoe", required = true)
    String username

    @Schema(description = "Date and time when the comment was created", example = "2023-01-15T16:30:15.123")
    LocalDateTime createdAt

    /**
     * Default constructor required by MongoDB.
     */
    Comment() {}

    /**
     * Creates a new comment with the specified content, user ID, and username.
     *
     * @param content The text content of the comment
     * @param userId The ID of the user creating the comment
     * @param username The username of the user creating the comment
     */
    Comment(String content, String userId, String username) {
        this.id = UUID.randomUUID().toString()
        this.content = content
        this.userId = userId
        this.username = username
        this.createdAt = LocalDateTime.now()
    }
}
