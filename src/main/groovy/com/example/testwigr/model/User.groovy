package com.example.testwigr.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Represents a user account in the system.
 * A user can create posts, follow other users, like posts, and add comments.
 */
@Document(collection = "users")
@Schema(description = "Represents a user account with profile information and social connections")
class User {
    @Id
    @Schema(description = "Unique identifier for the user", example = "60d21b4667d1d12d98a8e543")
    String id

    @Indexed(unique = true)
    @Schema(description = "Unique username for the user", example = "johndoe", required = true)
    String username

    @Indexed(unique = true)
    @Schema(description = "Unique email address for the user", example = "john.doe@example.com", required = true)
    String email

    @JsonIgnore
    @Schema(description = "User's password (encrypted)", hidden = true)
    String password

    @Schema(description = "User's display name shown in the UI", example = "John Doe")
    String displayName

    @Schema(description = "User's biographical information or profile description", example = "Software developer and tech enthusiast")
    String bio

    @ArraySchema(schema = @Schema(description = "ID of a user being followed", example = "60d21b4667d1d12d98a8e123"))
    @Schema(description = "Set of user IDs that this user follows")
    Set<String> following = []

    @ArraySchema(schema = @Schema(description = "ID of a follower user", example = "60d21b4667d1d12d98a8e456"))
    @Schema(description = "Set of user IDs that follow this user")
    Set<String> followers = []

    @Schema(description = "Date and time when the user account was created", example = "2023-01-10T12:30:15.123")
    LocalDateTime createdAt

    @Schema(description = "Date and time when the user profile was last updated", example = "2023-01-15T15:45:22.456")
    LocalDateTime updatedAt

    @Schema(description = "Indicates whether the user account is active", example = "true", defaultValue = "true")
    boolean active = true

    /**
     * Default constructor required by MongoDB.
     */
    User() {}

    /**
     * Creates a new user with the specified username, email, password, and display name.
     *
     * @param username The unique username for the user
     * @param email The unique email address for the user
     * @param password The user's password (will be encrypted before storage)
     * @param displayName The user's display name (defaults to username if not provided)
     */
    User(String username, String email, String password, String displayName) {
        this.username = username
        this.email = email
        this.password = password
        this.displayName = displayName
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Checks if this user is following another user.
     *
     * @param userId The ID of the user to check
     * @return true if this user follows the specified user, false otherwise
     */
    @Schema(description = "Checks if this user follows the specified user")
    boolean isFollowing(String userId) {
        return following.contains(userId)
    }

    /**
     * Gets the total number of users this user is following.
     *
     * @return The count of followed users
     */
    @Schema(description = "Number of users this user is following", example = "42")
    int getFollowingCount() {
        return following.size()
    }

    /**
     * Gets the total number of followers this user has.
     *
     * @return The count of followers
     */
    @Schema(description = "Number of users following this user", example = "128")
    int getFollowersCount() {
        return followers.size()
    }
}
