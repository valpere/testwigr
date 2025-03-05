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
            def user = TestDataFactory.createUser("dbuser${i}-id", "dbuser${i}")
            user.password = passwordEncoder.encode("password")
            users << userRepository.save(user)
        }

        // Create and save posts
        users.each { user ->
            postsPerUser.times { i ->
                def post = TestDataFactory.createPost("dbpost${user.id}-${i}", "Database test post ${i}", user.id, user.username)
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

        // Create users with predictable IDs
        def users = []
        userCount.times { i ->
            def user = TestDataFactory.createUser("netuser${i}-id", "netuser${i}")
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

        // Save all users to the database with consistent IDs
        def savedUsers = []
        scenarioData.users.each { user ->
            user.id = "scenario-user-${user.username}"
            user.password = passwordEncoder.encode("password")
            savedUsers << userRepository.save(user)
        }

        // Save all posts to the database with consistent IDs
        def savedPosts = []
        scenarioData.posts.eachWithIndex { post, index ->
            post.id = "scenario-post-${index}"
            savedPosts << postRepository.save(post)
        }

        return [users: savedUsers, posts: savedPosts]
    }

    // Clean database after tests
    static void cleanDatabase(UserRepository userRepository, PostRepository postRepository) {
        postRepository.deleteAll()
        userRepository.deleteAll()
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

        def user = TestDataFactory.createUser("test-user-${username}", username)
        user.password = passwordEncoder.encode("password")
        return userRepository.save(user)
    }
}
