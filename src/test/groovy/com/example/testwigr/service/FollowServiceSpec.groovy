package com.example.testwigr.service

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification

class FollowServiceSpec extends Specification {

    UserRepository userRepository
    PostRepository postRepository
    PasswordEncoder passwordEncoder
    UserService userService

    def setup() {
        userRepository = Mock(UserRepository)
        passwordEncoder = Mock(PasswordEncoder)
        postRepository = Mock(PostRepository)
        userService = new UserService(userRepository, passwordEncoder, postRepository)
    }

    def "should follow a user successfully"() {
        given:
        def followerId = "123"
        def followingId = "456"
        def follower = new User(id: followerId, username: "follower", following: [] as Set)
        def following = new User(id: followingId, username: "following", followers: [] as Set)

        and:
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_) >> { User user -> user }

        when:
        def result = userService.followUser(followerId, followingId)

        then:
        result.following.contains(followingId)
        1 * userRepository.save({ it.id == followingId && it.followers.contains(followerId) })
    }

    def "should unfollow a user successfully"() {
        given:
        def followerId = "123"
        def followingId = "456"
        def follower = new User(id: followerId, username: "follower", following: [followingId] as Set)
        def following = new User(id: followingId, username: "following", followers: [followerId] as Set)

        and:
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_) >> { User user -> user }

        when:
        def result = userService.unfollowUser(followerId, followingId)

        then:
        !result.following.contains(followingId)
        1 * userRepository.save({ it.id == followingId && !it.followers.contains(followerId) })
    }

    def "should get followers"() {
        given:
        def userId = "123"
        def followerId1 = "456"
        def followerId2 = "789"
        def user = new User(id: userId, username: "user", followers: [followerId1, followerId2] as Set)
        def follower1 = new User(id: followerId1, username: "follower1")
        def follower2 = new User(id: followerId2, username: "follower2")

        and:
        userRepository.findById(userId) >> Optional.of(user)
        userRepository.findById(followerId1) >> Optional.of(follower1)
        userRepository.findById(followerId2) >> Optional.of(follower2)

        when:
        def result = userService.getFollowers(userId)

        then:
        result.size() == 2
        result.any { it.id == followerId1 }
        result.any { it.id == followerId2 }
    }
}
