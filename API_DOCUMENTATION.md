# Testwigr API Documentation

## Introduction

This documentation provides comprehensive guidance for using the Testwigr API, a Twitter-like platform allowing users to create posts, follow other users, and interact through likes and comments.

The API is organized around REST principles. It accepts JSON-encoded request bodies, returns JSON-encoded responses, and uses standard HTTP response codes, authentication, and verbs.

## Base URL

All API endpoints are relative to the base URL:

**Development:** `http://localhost:8080`
**Testing:** `http://test.testwigr.example.com`
**Production:** `https://api.testwigr.example.com`

## Authentication

The Testwigr API uses JSON Web Tokens (JWT) for authentication. To use authenticated endpoints, you must include the token in the Authorization header of your request.

### Obtaining a Token

1. Register a new user account via the `/api/auth/register` endpoint
2. Log in via the `/api/auth/login` endpoint to receive a JWT token
3. Include the token in the Authorization header for subsequent requests

Example:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Expiration

JWT tokens are valid for 24 hours by default. After expiration, you must log in again to obtain a new token.

## Request Format

For POST and PUT requests, the request body should be in JSON format with the Content-Type header set to `application/json`.

Example request:
```json
{
  "content": "This is my first post on Testwigr!"
}
```

## Response Format

All responses are returned in JSON format. Successful responses typically include a status code of 200 OK and a response body containing the requested data.

Example success response:
```json
{
  "id": "60d21b4667d1d12d98a8e543",
  "content": "This is my first post on Testwigr!",
  "userId": "60d21b4667d1d12d98a8e123",
  "username": "johndoe",
  "createdAt": "2023-01-15T14:30:15.123",
  "updatedAt": "2023-01-15T14:30:15.123",
  "likes": [],
  "comments": []
}
```

## Error Handling

The API uses conventional HTTP response codes to indicate the success or failure of an API request:

- `200 OK`: The request was successful
- `400 Bad Request`: The request was invalid or cannot be served (e.g., validation error)
- `401 Unauthorized`: Authentication failed or user doesn't have permissions
- `403 Forbidden`: The request is understood but refused due to permissions
- `404 Not Found`: The requested resource does not exist
- `409 Conflict`: The request conflicts with current state (e.g., duplicate username)
- `5xx`: Server errors

Error responses include a JSON object with details about the error:

```json
{
  "message": "Username already taken",
  "details": "Request processing failed"
}
```

## Pagination

List endpoints support pagination using query parameters:

- `page`: Page number (0-indexed)
- `size`: Number of items per page
- `sort`: Property name to sort by (optionally followed by `,asc` or `,desc`)

Example: `/api/posts?page=0&size=10&sort=createdAt,desc`

Paginated responses include:

```json
{
  "content": [...],  // Array of items
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {...}
  },
  "totalElements": 42,
  "totalPages": 5,
  "last": false,
  "first": true,
  "empty": false
}
```

## API Endpoints

### Authentication

#### Register a new user

```
POST /api/auth/register
```

Request body:
```json
{
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "securepassword",
  "displayName": "John Doe"
}
```

Response:
```json
{
  "success": true,
  "userId": "60d21b4667d1d12d98a8e123",
  "username": "johndoe"
}
```

#### Log in

```
POST /api/auth/login
```

Request body:
```json
{
  "username": "johndoe",
  "password": "securepassword"
}
```

Response:
```json
{
  "success": true,
  "username": "johndoe",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Log out

```
POST /api/auth/logout
```

Response:
```json
{
  "success": true,
  "message": "User logged out successfully"
}
```

### Users

#### Get user by username

```
GET /api/users/{username}
```

Response:
```json
{
  "id": "60d21b4667d1d12d98a8e123",
  "username": "johndoe",
  "email": "john.doe@example.com",
  "displayName": "John Doe",
  "bio": "Software developer and tech enthusiast",
  "following": [...],
  "followers": [...],
  "createdAt": "2023-01-10T12:30:15.123",
  "updatedAt": "2023-01-15T15:45:22.456",
  "active": true,
  "followingCount": 42,
  "followersCount": 128
}
```

#### Get current user profile

```
GET /api/users/me
```

Response: Same as Get user by username

#### Update current user profile

```
PUT /api/users/me
```

Request body:
```json
{
  "displayName": "John Doe Updated",
  "bio": "Updated bio information",
  "email": "new.email@example.com",
  "password": "newpassword"
}
```

Response: Updated user object

#### Delete current user account

```
DELETE /api/users/me
```

Response:
```json
{
  "success": true,
  "message": "User deleted successfully"
}
```

### Follow Relationships

#### Follow a user

```
POST /api/follow/{followingId}
```

Response:
```json
{
  "success": true,
  "following": 43,
  "isFollowing": true
}
```

#### Unfollow a user

```
DELETE /api/follow/{followingId}
```

Response:
```json
{
  "success": true,
  "following": 42,
  "isFollowing": false
}
```

#### Get followers

```
GET /api/follow/followers
```

Response: Array of user objects

#### Get following

```
GET /api/follow/following
```

Response: Array of user objects

#### Get follow status

```
GET /api/follow/{userId}/status
```

Response:
```json
{
  "isFollowing": true,
  "isFollower": false,
  "followersCount": 128,
  "followingCount": 42
}
```

### Posts

#### Create a post

```
POST /api/posts
```

Request body:
```json
{
  "content": "This is my first post on Testwigr!"
}
```

Response: Created post object

#### Get a post by ID

```
GET /api/posts/{id}
```

Response: Post object

#### Get posts by user ID

```
GET /api/posts/user/{userId}
```

Response: Paginated list of posts

#### Update a post

```
PUT /api/posts/{id}
```

Request body:
```json
{
  "content": "Updated post content"
}
```

Response: Updated post object

#### Delete a post

```
DELETE /api/posts/{id}
```

Response:
```json
{
  "success": true,
  "message": "Post deleted successfully"
}
```

### Likes

#### Like a post

```
POST /api/likes/posts/{postId}
```

Response:
```json
{
  "success": true,
  "likeCount": 43,
  "isLiked": true
}
```

#### Unlike a post

```
DELETE /api/likes/posts/{postId}
```

Response:
```json
{
  "success": true,
  "likeCount": 42,
  "isLiked": false
}
```

#### Get like status

```
GET /api/likes/posts/{postId}
```

Response:
```json
{
  "likeCount": 42,
  "isLiked": true
}
```

#### Get users who liked a post

```
GET /api/likes/posts/{postId}/users
```

Response: Array of user objects

### Comments

#### Add a comment to a post

```
POST /api/comments/posts/{postId}
```

Request body:
```json
{
  "content": "Great post! Thanks for sharing."
}
```

Response: Updated post object

#### Get comments for a post

```
GET /api/comments/posts/{postId}
```

Response: Array of comment objects

### Feed

#### Get personal feed

```
GET /api/feed
```

Response: Paginated list of posts from followed users and self

#### Get user feed

```
GET /api/feed/users/{username}
```

Response: Paginated list of posts from the specified user

## Rate Limiting

The API implements rate limiting to prevent abuse. If you exceed the rate limit, you'll receive a 429 Too Many Requests response. The response headers will include:

- `X-RateLimit-Limit`: The maximum number of requests you're allowed to make per hour
- `X-RateLimit-Remaining`: The number of requests remaining in the current rate limit window
- `X-RateLimit-Reset`: The time at which the current rate limit window resets in UTC epoch seconds

## Versioning

The current API version is v1. We recommend explicitly versioning API calls to ensure compatibility as the API evolves.

## API Changelog

### v1.0.0 (Current)
- Initial release with user authentication, posts, follows, likes, and comments functionality
- Feed generation for personal and user-specific timelines

## Support

For API support, please email api-support@testwigr.example.com or visit the [Developer Forum](https://developers.testwigr.example.com).

## License

This API is licensed under the Apache License 2.0. See the LICENSE file for details.
