package com.example.testwigr.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "posts")
class Post {
    @Id
    String id
    
    String content
    
    String userId
    
    String username
    
    Set<String> likes = []
    
    List<Comment> comments = []
    
    LocalDateTime createdAt
    
    LocalDateTime updatedAt
    
    Post() {}
    
    Post(String content, String userId, String username) {
        this.content = content
        this.userId = userId
        this.username = username
        this.createdAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    boolean isLikedBy(String userId) {
        return likes.contains(userId)
    }
    
    int getLikeCount() {
        return likes.size()
    }
    
    int getCommentCount() {
        return comments.size()
    }

}
