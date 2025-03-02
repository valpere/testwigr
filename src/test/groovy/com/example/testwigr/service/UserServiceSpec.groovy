package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.exception.UserAlreadyExistsException
import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification

class UserServiceSpec extends Specification {

    UserRepository userRepository

    PasswordEncoder passwordEncoder
    UserService userService

    def setup() {
        userRepository = Mock(UserRepository)
        passwordEncoder = Mock(PasswordEncoder)
        userService = new UserService(userRepository, passwordEncoder)
    }

    def "should create a new user successfully"() {
        given:
        def userToCreate = new User(
            username: 'testuser',
            email: 'test@example.com',
            password: 'password123',
            displayName: 'Test User'
        )

        and:
        userRepository.existsByUsername('testuser') >> false
        userRepository.existsByEmail('test@example.com') >> false
        passwordEncoder.encode('password123') >> 'encodedPassword'
        userRepository.save(_ as User) >> { User user -> user }

        when:
        def result = userService.createUser(userToCreate)

        then:
        result.username == 'testuser'
        result.email == 'test@example.com'
        result.password == 'encodedPassword'
        result.displayName == 'Test User'
    }

    def "should throw exception when username already exists"() {
        given:
        def userToCreate = new User(
            username: 'existinguser',
            email: 'new@example.com',
            password: 'password123'
        )

        and:
        userRepository.existsByUsername('existinguser') >> true

        when:
        userService.createUser(userToCreate)

        then:
        thrown(UserAlreadyExistsException)
    }

    def "should throw exception when email already exists"() {
        given:
        def userToCreate = new User(
            username: 'newuser',
            email: 'existing@example.com',
            password: 'password123'
        )

        and:
        userRepository.existsByUsername('newuser') >> false
        userRepository.existsByEmail('existing@example.com') >> true

        when:
        userService.createUser(userToCreate)

        then:
        thrown(UserAlreadyExistsException)
    }

    def "should get user by id"() {
        given:
        def userId = '123'
        def user = new User(id: userId, username: 'testuser')

        and:
        userRepository.findById(userId) >> Optional.of(user)

        when:
        def result = userService.getUserById(userId)

        then:
        result.id == userId
        result.username == 'testuser'
    }

    def "should throw exception when user id not found"() {
        given:
        def userId = 'nonexistent'

        and:
        userRepository.findById(userId) >> Optional.empty()

        when:
        userService.getUserById(userId)

        then:
        thrown(ResourceNotFoundException)
    }

    def "should follow a user successfully"() {
        given:
        def followerId = '123'
        def followingId = '456'
        def follower = new User(id: followerId, username: 'follower', following: [] as Set, followers: [] as Set)
        def following = new User(id: followingId, username: 'following', following: [] as Set, followers: [] as Set)

        and:
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_ as User) >> { User user -> user }

        when:
        def result = userService.followUser(followerId, followingId)

        then:
        result.following.contains(followingId)
        1 * userRepository.save({ it.followers.contains(followerId) })
    }

    def "should unfollow a user successfully"() {
        given:
        def followerId = '123'
        def followingId = '456'
        def follower = new User(id: followerId, username: 'follower', following: [followingId] as Set)
        def following = new User(id: followingId, username: 'following', followers: [followerId] as Set)

        and:
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_ as User) >> { User user -> user }

        when:
        def result = userService.unfollowUser(followerId, followingId)

        then:
        !result.following.contains(followingId)
        1 * userRepository.save({ it.id == followingId && !it.followers.contains(followerId) })
    }
}
