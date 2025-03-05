package com.example.testwigr.repository

import com.example.testwigr.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository extends MongoRepository<User, String> {
    // Find user with minimal information (for authentication)
    @Query(value = "{ 'username': ?0 }", fields = "{ 'password': 1, 'active': 1, 'username': 1, 'id': 1 }")
    Optional<User> findByUsernameForAuth(String username)

    // Find user with profile information but exclude sensitive fields
    @Query(value = "{ 'username': ?0 }", fields = "{ 'password': 0 }")
    Optional<User> findByUsernameWithoutPassword(String username)

    // Find users by IDs for profile lookup (exclude password and sensitive info)
    @Query(value = "{ '_id': { '\$in': ?0 } }", fields = "{ 'password': 0 }")
    List<User> findByIdInWithoutPassword(Collection<String> ids)

    // Find followers with pagination
    @Query(value = "{ '_id': { '\$in': ?0 } }", fields = "{ 'password': 0 }")
    Page<User> findFollowersByIds(Collection<String> followerIds, Pageable pageable)

    // Original methods
    Optional<User> findByUsername(String username)
    Optional<User> findByEmail(String email)
    boolean existsByUsername(String username)
    boolean existsByEmail(String email)

}
