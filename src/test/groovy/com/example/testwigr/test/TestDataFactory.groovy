package com.example.testwigr.test

import com.example.testwigr.model.Comment
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import java.time.LocalDateTime
import java.util.UUID

class TestDataFactory {

    // User Factory Methods
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
            active: true,
            bio: "Bio for ${username}"
        )
        if (id) {
            user.id = id
        }
        return user
    }

    // Create a user with followers and following
    static User createUserWithConnections(String id = null, String username = 'networkuser',
                                         List<String> followerIds = [], List<String> followingIds = []) {
        def user = createUser(id, username)
        user.followers.addAll(followerIds)
        user.following.addAll(followingIds)
        return user
    }

    // Post Factory Methods
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

    // Create a post with likes
    static Post createPostWithLikes(String id = null, String content = 'Popular post',
                                  String userId = '123', String username = 'testuser',
                                  Set<String> likeUserIds = []) {
        def post = createPost(id, content, userId, username)
        post.likes.addAll(likeUserIds)
        return post
    }

    // Create a post with comments
    static Post createPostWithComments(String id = null, String content = 'Commented post',
                                     String userId = '123', String username = 'testuser',
                                     List<Comment> commentsToAdd = []) {
        def post = createPost(id, content, userId, username)
        post.comments.addAll(commentsToAdd)
        return post
    }

    // Comment Factory Methods
    static Comment createComment(String content = 'Test comment', String userId = '123', String username = 'testuser') {
        return new Comment(content, userId, username)
    }

    // Create multiple comments
    static List<Comment> createComments(int count = 3, String baseContent = 'Comment',
                                        String userId = '123', String username = 'testuser') {
        def comments = []
        count.times { i ->
            comments << createComment("${baseContent} ${i+1}", userId, username)
        }
        return comments
    }

    // Create a fully populated social scenario (users, posts, comments, likes)
    static Map<String, Object> createSocialScenario(int userCount = 3, int postsPerUser = 2,
                                                 int commentsPerPost = 2, int likesPerPost = 2) {
        def users = []
        def posts = []
        def result = [users: users, posts: posts]

        // Create users
        userCount.times { i ->
            def user = createUser(UUID.randomUUID().toString(), "user${i}")
            users << user
        }

        // Create posts, comments and likes
        users.each { user ->
            postsPerUser.times { i ->
                def post = createPost(UUID.randomUUID().toString(), "Post ${i} from ${user.username}", user.id, user.username)

                // Add comments from random users
                commentsPerPost.times { c ->
                    def commenter = users[c % users.size()]
                    post.comments << createComment("Comment on ${user.username}'s post", commenter.id, commenter.username)
                }

                // Add likes from random users
                likesPerPost.times { l ->
                    def liker = users[l % users.size()]
                    post.likes.add(liker.id)
                }

                posts << post
            }
        }

        return result
    }

}
