package com.example.testwigr.test

import com.example.testwigr.model.Comment
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import java.time.LocalDateTime

class TestDataFactory {

    static User createUser(String id = null, String username = 'testuser') {
        def user = new User(
            username: username,
            email: "${username}@example.com",
            password: 'password123',
            displayName: username.capitalize(),
            createdAt: LocalDateTime.now(),
            updatedAt: LocalDateTime.now(),
            following: [] as Set,
            followers: [] as Set,
            active: true
        )
        if (id) {
            user.id = id
        }
        return user
    }

    static Post createPost(String id = null, String content = 'Test post', String userId = '123', String username = 'testuser') {
        def post = new Post(
            content: content,
            userId: userId,
            username: username,
            likes: [] as Set,
            comments: [],
            createdAt: LocalDateTime.now(),
            updatedAt: LocalDateTime.now()
        )
        if (id) {
            post.id = id
        }
        return post
    }

    static Comment createComment(String content = 'Test comment', String userId = '123', String username = 'testuser') {
        return new Comment(content, userId, username)
    }
}
