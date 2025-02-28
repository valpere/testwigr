package com.example.testwigr.model

import java.time.LocalDateTime

class Comment {
    String id
    
    String content
    
    String userId
    
    String username
    
    LocalDateTime createdAt
    
    Comment() {}
    
    Comment(String content, String userId, String username) {
        this.id = UUID.randomUUID().toString()
        this.content = content
        this.userId = userId
        this.username = username
        this.createdAt = LocalDateTime.now()
    }
}
