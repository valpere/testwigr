package com.example.testwigr.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "users")
class User {
    @Id
    String id
    
    @Indexed(unique = true)
    String username
    
    @Indexed(unique = true)
    String email
    
    String password
    
    String displayName
    
    String bio
    
    Set<String> following = []
    
    Set<String> followers = []
    
    LocalDateTime createdAt
    
    LocalDateTime updatedAt
    
    boolean active = true
    
    User() {}
    
    User(String username, String email, String password, String displayName) {
        this.username = username
        this.email = email
        this.password = password
        this.displayName = displayName
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    boolean isFollowing(String userId) {
        return following.contains(userId)
    }
}
