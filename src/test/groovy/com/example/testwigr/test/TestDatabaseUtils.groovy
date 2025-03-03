package com.example.testwigr.test

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.data.mongodb.core.query.Query

class TestDatabaseUtils {

    // Populate test database with users and posts
    static void populateDatabase(UserRepository userRepository,
                                PostRepository postRepository,
                                PasswordEncoder passwordEncoder,
                                int userCount = 3,
                                int postsPerUser = 2) {
        // Clear existing data
        cleanDatabase(userRepository, postRepository)

        // Create and save users
        def users = []
        userCount.times { i ->
            def user = TestDataFactory.createUser(null, "dbuser${i}")
            user.password = passwordEncoder.encode("password")
            users << userRepository.save(user)
        }

        // Create and save posts
        users.each { user ->
            postsPerUser.times { i ->
                def post = TestDataFactory.createPost(null, "Database test post ${i}", user.id, user.username)
                postRepository.save(post)
            }
        }
    }

    // Create a social network with follows
    static void createSocialNetwork(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   int userCount = 5) {
        // Clear existing users
        userRepository.deleteAll()

        // Create users
        def users = []
        userCount.times { i ->
            def user = TestDataFactory.createUser(null, "netuser${i}")
            user.password = passwordEncoder.encode("password")
            users << userRepository.save(user)
        }

        // Create a follow network (each user follows some others)
        users.eachWithIndex { user, idx ->
            // Each user follows 2 other users (with wraparound)
            def firstFollowIdx = (idx + 1) % userCount
            def secondFollowIdx = (idx + 2) % userCount

            user.following.add(users[firstFollowIdx].id)
            user.following.add(users[secondFollowIdx].id)

            users[firstFollowIdx].followers.add(user.id)
            users[secondFollowIdx].followers.add(user.id)

            userRepository.save(user)
            userRepository.save(users[firstFollowIdx])
            userRepository.save(users[secondFollowIdx])
        }
    }
    
    // Create a complex database scenario with realistic data patterns
    static Map<String, Object> createComplexDatabaseScenario(
            UserRepository userRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder,
            int userCount = 10) {
            
        // Clean database first
        cleanDatabase(userRepository, postRepository)
        
        // Create complex social network
        def scenarioData = TestDataFactory.createComplexSocialNetwork(userCount)
        
        // Save all users to the database
        def savedUsers = []
        scenarioData.users.each { user ->
            user.password = passwordEncoder.encode("password")
            savedUsers << userRepository.save(user)
        }
        
        // Save all posts to the database
        def savedPosts = []
        scenarioData.posts.each { post ->
            savedPosts << postRepository.save(post)
        }
        
        return [users: savedUsers, posts: savedPosts]
    }
    
    // Populate database with specific test cases for edge cases
    static void populateWithTestCases(
            UserRepository userRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder) {
        
        // Clean database first
        cleanDatabase(userRepository, postRepository)
        
        // 1. Create an inactive user
        def inactiveUser = TestDataFactory.createInactiveUser()
        inactiveUser.password = passwordEncoder.encode("password")
        userRepository.save(inactiveUser)
        
        // 2. Create a user with very long content
        def longContentUser = TestDataFactory.createUser(null, "longcontent")
        longContentUser.bio = "A" * 500 // Very long bio
        longContentUser.password = passwordEncoder.encode("password")
        userRepository.save(longContentUser)
        
        // 3. Create a post with maximum length content
        def user = TestDataFactory.createUser()
        user.password = passwordEncoder.encode("password")
        def savedUser = userRepository.save(user)
        
        def maxLengthPost = TestDataFactory.createPost(
            null,
            "X" * 280, // Maximum Twitter-like length
            savedUser.id,
            savedUser.username
        )
        postRepository.save(maxLengthPost)
        
        // 4. Create a post with many comments
        def commentedPost = TestDataFactory.createPost(
            null,
            "Post with many comments",
            savedUser.id,
            savedUser.username
        )
        
        // Add 20 comments from the same user
        20.times { i ->
            commentedPost.comments << TestDataFactory.createComment(
                "Comment ${i}",
                savedUser.id,
                savedUser.username
            )
        }
        
        postRepository.save(commentedPost)
        
        // 5. Create a post with many likes
        def popularPost = TestDataFactory.createPost(
            null,
            "Very popular post",
            savedUser.id,
            savedUser.username
        )
        
        // Create 25 users who like the post
        25.times { i ->
            def liker = TestDataFactory.createUser(null, "liker${i}")
            liker.password = passwordEncoder.encode("password")
            def savedLiker = userRepository.save(liker)
            
            popularPost.likes.add(savedLiker.id)
        }
        
        postRepository.save(popularPost)
    }

    // Clean database after tests
    static void cleanDatabase(UserRepository userRepository, PostRepository postRepository) {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }
    
    // Drop and recreate collections (more thorough cleaning than just deleteAll)
    static void dropCollections(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection(User.class)
        mongoTemplate.dropCollection(Post.class)
    }
    
    // Count objects in collections
    static Map<String, Integer> getDatabaseStats(UserRepository userRepository, PostRepository postRepository) {
        return [
            userCount: userRepository.count(),
            postCount: postRepository.count()
        ]
    }
    
    // Create a specific user with access to the database for testing
    static User createTestUser(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder,
            String username = "testuser") {
            
        def existingUser = userRepository.findByUsername(username)
        if (existingUser.isPresent()) {
            return existingUser.get()
        }
        
        def user = TestDataFactory.createUser(null, username)
        user.password = passwordEncoder.encode("password")
        return userRepository.save(user)
    }
    
    // Create an authenticated user in the database
    static User createAuthenticatedUser(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder) {
            
        def user = createTestUser(userRepository, passwordEncoder, "authuser")
        return user
    }
}