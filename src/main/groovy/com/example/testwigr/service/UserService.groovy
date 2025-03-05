package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.exception.UserAlreadyExistsException
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserService implements UserDetailsService {

    private final UserRepository userRepository
    private final PasswordEncoder passwordEncoder
    private final PostRepository postRepository

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PostRepository postRepository) {
        this.userRepository = userRepository
        this.passwordEncoder = passwordEncoder
        this.postRepository = postRepository
    }
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException('User not found with username: ' + username))

        return org.springframework.security.core.userdetails.User
                .withUsername(user.username)
                .password(user.password)
                .authorities([])
                .accountExpired(!user.active)
                .accountLocked(!user.active)
                .credentialsExpired(false)
                .disabled(!user.active)
                .build()
    }

    User createUser(User userRequest) {
        if (userRepository.existsByUsername(userRequest.username)) {
            throw new UserAlreadyExistsException('Username already taken')
        }

        if (userRepository.existsByEmail(userRequest.email)) {
            throw new UserAlreadyExistsException('Email already in use')
        }

        User user = new User(
                userRequest.username,
                userRequest.email,
                passwordEncoder.encode(userRequest.password),
                userRequest.displayName ?: userRequest.username
        )

        return userRepository.save(user)
    }

    // Use method name as part of cache key instead of SpEL
    @Cacheable("userById")
    User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException('User not found with id: ' + id))
    }

    // Use method name as part of cache key instead of SpEL
    @Cacheable("userProfile")
    User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException('User not found with username: ' + username))
    }

    // Simplify cache eviction
    @CacheEvict(value = ["userProfile", "userById"], allEntries = true)
    User updateUser(String id, User userDetails) {
        User user = getUserById(id)

        if (userDetails.displayName) {
            user.displayName = userDetails.displayName
        }

        if (userDetails.bio) {
            user.bio = userDetails.bio
        }

        if (userDetails.email && !user.email.equals(userDetails.email)) {
            if (userRepository.existsByEmail(userDetails.email)) {
                throw new UserAlreadyExistsException('Email already in use')
            }
            user.email = userDetails.email
        }

        if (userDetails.password) {
            user.password = passwordEncoder.encode(userDetails.password)
        }

        user.updatedAt = LocalDateTime.now()

        return userRepository.save(user)
    }

    @CacheEvict(value = ["userProfile", "userById"], allEntries = true)
    void deleteUser(String id) {
        User user = getUserById(id)
        user.active = false
        userRepository.save(user)
    }

    User followUser(String followerId, String followingId) {
        if (followerId == followingId) {
            throw new IllegalArgumentException('You cannot follow yourself')
        }

        User follower = getUserById(followerId)
        User following = getUserById(followingId)

        follower.following.add(followingId)
        following.followers.add(followerId)

        userRepository.save(following)
        return userRepository.save(follower)
    }

    User unfollowUser(String followerId, String followingId) {
        User follower = getUserById(followerId)
        User following = getUserById(followingId)

        follower.following.remove(followingId)
        following.followers.remove(followerId)

        userRepository.save(following)
        return userRepository.save(follower)
    }

    List<User> getFollowers(String userId) {
        User user = getUserById(userId)

        List<User> followers = []
        for (String followerId : user.followers) {
            try {
                User follower = getUserById(followerId)
                followers.add(follower)
            } catch (ResourceNotFoundException e) {
                // Skip users that might have been deleted
            }
        }

        return followers
    }

    List<User> getFollowing(String userId) {
        User user = getUserById(userId)

        List<User> following = []
        for (String followingId : user.following) {
            try {
                User followingUser = getUserById(followingId)
                following.add(followingUser)
            } catch (ResourceNotFoundException e) {
                // Skip users that might have been deleted
            }
        }

        return following
    }

    // New paginated methods
    Page<User> getFollowersPage(String userId, Pageable pageable) {
        User user = getUserById(userId)

        if (user.followers.isEmpty()) {
            return new PageImpl<>([], pageable, 0)
        }

        // If the repository has the paginated method
        if (userRepository.respondsTo('findFollowersByIds')) {
            return userRepository.findFollowersByIds(user.followers, pageable)
        }

        // Fallback implementation if repository method isn't available
        List<User> followers = []
        for (String followerId : user.followers) {
            try {
                User follower = getUserById(followerId)
                followers.add(follower)
            } catch (ResourceNotFoundException e) {
                // Skip users that might have been deleted
            }
        }

        // Manual pagination
        int start = (int) pageable.getOffset()
        int end = Math.min((start + pageable.getPageSize()), followers.size())

        List<User> pageContent = start < end ? followers.subList(start, end) : []

        return new PageImpl<>(pageContent, pageable, followers.size())
    }

    Page<User> getFollowingPage(String userId, Pageable pageable) {
        User user = getUserById(userId)

        if (user.following.isEmpty()) {
            return new PageImpl<>([], pageable, 0)
        }

        // If the repository has the paginated method
        if (userRepository.respondsTo('findFollowersByIds')) {
            return userRepository.findFollowersByIds(user.following, pageable)
        }

        // Fallback implementation if repository method isn't available
        List<User> following = []
        for (String followingId : user.following) {
            try {
                User followingUser = getUserById(followingId)
                following.add(followingUser)
            } catch (ResourceNotFoundException e) {
                // Skip users that might have been deleted
            }
        }

        // Manual pagination
        int start = (int) pageable.getOffset()
        int end = Math.min((start + pageable.getPageSize()), following.size())

        List<User> pageContent = start < end ? following.subList(start, end) : []

        return new PageImpl<>(pageContent, pageable, following.size())
    }

    // Use method name as cache key
    @Cacheable("postCount")
    long getUserPostCount(String userId) {
        // Verify user exists
        getUserById(userId)
        return postRepository.countByUserId(userId)
    }

}
