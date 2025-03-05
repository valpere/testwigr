package com.example.testwigr.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = 'posts')
@CompoundIndexes([
    @CompoundIndex(name = 'userId_createdAt_idx', def = "{'userId': 1, 'createdAt': -1}")
])
class Post {

    @Id
    String id

    String content

    @Indexed
    String userId

    String username

    Set<String> likes = []

    List<Comment> comments = []

    @Indexed
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
