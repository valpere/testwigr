package com.example.testwigr.test

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder

class TestDatabaseUtils {

    // Populate test database with users and posts
    static void populateDatabase(UserRepository userRepository,
                                PostRepository postRepository,
                                PasswordEncoder passwordEncoder,
                                int userCount = 3,
                                int postsPerUser = 2) {
        // Clear existing data
        postRepository.deleteAll()
        userRepository.deleteAll()

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

    // Clean database after tests
    static void cleanDatabase(UserRepository userRepository, PostRepository postRepository) {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

}
