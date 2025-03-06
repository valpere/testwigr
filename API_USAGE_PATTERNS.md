# Testwigr API: Common Usage Patterns

This document provides code examples and guidance for common API usage patterns across different programming languages and platforms.

## Table of Contents

- [Authentication Flows](#authentication-flows)
- [User Management](#user-management)
- [Content Creation](#content-creation)
- [Social Interactions](#social-interactions)
- [Feed Management](#feed-management)
- [Error Handling](#error-handling)
- [Pagination](#pagination)
- [Rate Limiting](#rate-limiting)
- [Versioning](#versioning)

## Authentication Flows

### Registration and Login Flow

The typical authentication flow involves registering an account and then logging in to obtain a JWT token.

#### Example (JavaScript)

```javascript
// Register a new user
async function registerUser(userData) {
  const response = await fetch('https://api.testwigr.example.com/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData),
  });
  return response.json();
}

// Login and get token
async function login(username, password) {
  const response = await fetch('https://api.testwigr.example.com/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  });
  const data = await response.json();
  
  // Store the token for later use
  if (data.token) {
    localStorage.setItem('authToken', data.token);
  }
  
  return data;
}

// Example usage
const userData = {
  username: 'johndoe',
  email: 'john.doe@example.com',
  password: 'securepassword',
  displayName: 'John Doe'
};

// First register, then login
registerUser(userData)
  .then(() => login(userData.username, userData.password))
  .then(data => console.log('Logged in successfully:', data))
  .catch(error => console.error('Authentication error:', error));
```

#### Example (Python)

```python
import requests

API_BASE_URL = "https://api.testwigr.example.com"

def register_user(username, email, password, display_name):
    """Register a new user account."""
    response = requests.post(
        f"{API_BASE_URL}/api/auth/register",
        json={
            "username": username,
            "email": email,
            "password": password,
            "displayName": display_name
        }
    )
    return response.json()

def login(username, password):
    """Login and get an authentication token."""
    response = requests.post(
        f"{API_BASE_URL}/api/auth/login",
        json={
            "username": username,
            "password": password
        }
    )
    return response.json()

# Example usage
user_data = {
    "username": "janedoe",
    "email": "jane.doe@example.com",
    "password": "securepassword",
    "display_name": "Jane Doe"
}

# Register and then login
register_response = register_user(**user_data)
print(f"Registration response: {register_response}")

if register_response.get("success", False):
    login_response = login(user_data["username"], user_data["password"])
    token = login_response.get("token")
    print(f"Login successful, token: {token}")
```

### Using the Authentication Token

Once you have obtained a token, include it in the `Authorization` header for all authenticated requests:

#### Example (JavaScript)

```javascript
// Function to make authenticated API requests
async function fetchWithAuth(url, options = {}) {
  const token = localStorage.getItem('authToken');
  
  const headers = {
    ...options.headers,
    'Authorization': `Bearer ${token}`,
  };
  
  return fetch(url, {
    ...options,
    headers,
  });
}

// Example: Get current user profile
async function getCurrentUserProfile() {
  const response = await fetchWithAuth('https://api.testwigr.example.com/api/users/me');
  return response.json();
}

// Example usage
getCurrentUserProfile()
  .then(profile => console.log('User profile:', profile))
  .catch(error => console.error('Error fetching profile:', error));
```

#### Example (Python)

```python
class TestwigrClient:
    """Client for interacting with the Testwigr API."""
    
    def __init__(self, base_url="https://api.testwigr.example.com"):
        self.base_url = base_url
        self.token = None
    
    def login(self, username, password):
        """Login and store the authentication token."""
        response = requests.post(
            f"{self.base_url}/api/auth/login",
            json={
                "username": username,
                "password": password
            }
        )
        data = response.json()
        if "token" in data:
            self.token = data["token"]
        return data
    
    def _get_headers(self):
        """Get headers including the authentication token if available."""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers
    
    def get_current_user(self):
        """Get the current user's profile."""
        response = requests.get(
            f"{self.base_url}/api/users/me",
            headers=self._get_headers()
        )
        return response.json()

# Example usage
client = TestwigrClient()
client.login("janedoe", "securepassword")
user_profile = client.get_current_user()
print(f"User profile: {user_profile}")
```

## User Management

### Updating User Profile

#### Example (JavaScript)

```javascript
async function updateUserProfile(profileData) {
  const response = await fetchWithAuth('https://api.testwigr.example.com/api/users/me', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(profileData),
  });
  return response.json();
}

// Example usage
updateUserProfile({
  displayName: 'John Doe Updated',
  bio: 'Software developer and tech enthusiast'
})
  .then(updatedProfile => console.log('Profile updated:', updatedProfile))
  .catch(error => console.error('Error updating profile:', error));
```

### Following/Unfollowing Users

#### Example (Python)

```python
def follow_user(client, user_id):
    """Follow a user."""
    response = requests.post(
        f"{client.base_url}/api/follow/{user_id}",
        headers=client._get_headers()
    )
    return response.json()

def unfollow_user(client, user_id):
    """Unfollow a user."""
    response = requests.delete(
        f"{client.base_url}/api/follow/{user_id}",
        headers=client._get_headers()
    )
    return response.json()

# Example usage
user_to_follow = "abcd1234"  # User ID
follow_response = follow_user(client, user_to_follow)
print(f"Follow response: {follow_response}")

# Later, unfollow the user
unfollow_response = unfollow_user(client, user_to_follow)
print(f"Unfollow response: {unfollow_response}")
```

## Content Creation

### Creating and Retrieving Posts

#### Example (JavaScript)

```javascript
// Create a new post
async function createPost(content) {
  const response = await fetchWithAuth('https://api.testwigr.example.com/api/posts', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ content }),
  });
  return response.json();
}

// Get a post by ID
async function getPost(postId) {
  const response = await fetchWithAuth(`https://api.testwigr.example.com/api/posts/${postId}`);
  return response.json();
}

// Example usage
createPost("Hello world! This is my first post on Testwigr.")
  .then(post => {
    console.log('Post created:', post);
    return getPost(post.id);
  })
  .then(post => console.log('Retrieved post:', post))
  .catch(error => console.error('Error:', error));
```

### Adding Comments to Posts

#### Example (Python)

```python
def add_comment(client, post_id, comment_content):
    """Add a comment to a post."""
    response = requests.post(
        f"{client.base_url}/api/comments/posts/{post_id}",
        headers=client._get_headers(),
        json={"content": comment_content}
    )
    return response.json()

def get_comments(client, post_id):
    """Get all comments for a post."""
    response = requests.get(
        f"{client.base_url}/api/comments/posts/{post_id}",
        headers=client._get_headers()
    )
    return response.json()

# Example usage
post_id = "post123"
comment_response = add_comment(client, post_id, "Great post! Thanks for sharing.")
print(f"Comment added: {comment_response}")

comments = get_comments(client, post_id)
print(f"Comments on post: {comments}")
```

## Social Interactions

### Liking and Unliking Posts

#### Example (JavaScript)

```javascript
// Like a post
async function likePost(postId) {
  const response = await fetchWithAuth(`https://api.testwigr.example.com/api/likes/posts/${postId}`, {
    method: 'POST',
  });
  return response.json();
}

// Unlike a post
async function unlikePost(postId) {
  const response = await fetchWithAuth(`https://api.testwigr.example.com/api/likes/posts/${postId}`, {
    method: 'DELETE',
  });
  return response.json();
}

// Example usage
const postId = 'abc123';

likePost(postId)
  .then(result => {
    console.log('Post liked:', result);
    
    // Later, unlike the post
    setTimeout(() => {
      unlikePost(postId)
        .then(result => console.log('Post unliked:', result))
        .catch(error => console.error('Error unliking post:', error));
    }, 10000);
  })
  .catch(error => console.error('Error liking post:', error));
```

### Getting Users Who Liked a Post

#### Example (Python)

```python
def get_post_likes(client, post_id):
    """Get users who liked a post."""
    response = requests.get(
        f"{client.base_url}/api/likes/posts/{post_id}/users",
        headers=client._get_headers()
    )
    return response.json()

# Example usage
post_id = "post123"
likes = get_post_likes(client, post_id)
print(f"Users who liked the post: {likes}")
```

## Feed Management

### Getting Personal Feed

#### Example (JavaScript)

```javascript
// Get personal feed (posts from followed users and self)
async function getPersonalFeed(page = 0, size = 20) {
  const response = await fetchWithAuth(
    `https://api.testwigr.example.com/api/feed?page=${page}&size=${size}&sort=createdAt,desc`
  );
  return response.json();
}

// Example usage with pagination
getPersonalFeed(0, 10)
  .then(feed => {
    console.log('Feed page 1:', feed);
    
    // If there are more pages, get the next page
    if (!feed.last) {
      return getPersonalFeed(1, 10);
    }
  })
  .then(nextPageFeed => {
    if (nextPageFeed) {
      console.log('Feed page 2:', nextPageFeed);
    }
  })
  .catch(error => console.error('Error fetching feed:', error));
```

### Getting User-Specific Feed

#### Example (Python)

```python
def get_user_feed(client, username, page=0, size=20):
    """Get posts from a specific user."""
    response = requests.get(
        f"{client.base_url}/api/feed/users/{username}",
        headers=client._get_headers(),
        params={
            "page": page,
            "size": size,
            "sort": "createdAt,desc"
        }
    )
    return response.json()

# Example usage
username = "johndoe"
user_feed = get_user_feed(client, username)
print(f"Posts from {username}: {user_feed}")

# Get the next page if available
if not user_feed.get("last", True):
    next_page = get_user_feed(client, username, page=1)
    print(f"More posts from {username}: {next_page}")
```

## Error Handling

### Common Error Patterns

#### Example (JavaScript)

```javascript
// Enhanced fetchWithAuth function with error handling
async function fetchWithAuth(url, options = {}) {
  const token = localStorage.getItem('authToken');
  
  const headers = {
    ...options.headers,
    'Authorization': `Bearer ${token}`,
  };
  
  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });
    
    const data = await response.json();
    
    // Handle API errors
    if (!response.ok) {
      const error = new Error(data.message || 'An error occurred');
      error.status = response.status;
      error.data = data;
      throw error;
    }
    
    return data;
  } catch (error) {
    // Handle network errors
    if (!error.status) {
      error.message = 'Network error: Please check your connection';
    }
    
    // Handle specific error cases
    switch (error.status) {
      case 401:
        // Unauthorized: Token might be expired
        localStorage.removeItem('authToken');
        // Redirect to login page or trigger re-authentication
        break;
      case 403:
        // Forbidden: User doesn't have permission
        console.error('Permission denied');
        break;
      case 404:
        // Not Found: Resource doesn't exist
        console.error('Resource not found');
        break;
      case 429:
        // Too Many Requests: Rate limit exceeded
        const retryAfter = error.headers?.get('Retry-After') || 60;
        console.error(`Rate limit exceeded. Try again in ${retryAfter} seconds.`);
        break;
    }
    
    throw error;
  }
}
```

#### Example (Python)

```python
class ApiError(Exception):
    """Exception raised for API errors."""
    
    def __init__(self, status_code, message, details=None):
        self.status_code = status_code
        self.message = message
        self.details = details
        super().__init__(f"{status_code}: {message}")

class TestwigrClient:
    # ... previous code ...
    
    def _request(self, method, endpoint, data=None, params=None):
        """Make an API request with error handling."""
        url = f"{self.base_url}{endpoint}"
        headers = self._get_headers()
        
        try:
            if method == 'GET':
                response = requests.get(url, headers=headers, params=params)
            elif method == 'POST':
                response = requests.post(url, headers=headers, json=data, params=params)
            elif method == 'PUT':
                response = requests.put(url, headers=headers, json=data, params=params)
            elif method == 'DELETE':
                response = requests.delete(url, headers=headers, params=params)
            else:
                raise ValueError(f"Unsupported HTTP method: {method}")
            
            # Parse response
            json_data = response.json()
            
            # Handle API errors
            if not response.ok:
                raise ApiError(
                    response.status_code,
                    json_data.get('message', 'An error occurred'),
                    json_data.get('error')
                )
            
            return json_data
        
        except requests.exceptions.ConnectionError:
            raise ApiError(0, "Network error: Unable to connect to API")
        except requests.exceptions.Timeout:
            raise ApiError(0, "Network error: Request timed out")
        except requests.exceptions.RequestException as e:
            raise ApiError(0, f"Network error: {str(e)}")
        except ValueError:
            raise ApiError(0, "Invalid response: Unable to parse JSON")
```

## Pagination

### Handling Paginated Results

#### Example (JavaScript)

```javascript
// Function to handle paginated API endpoints
async function fetchAllPages(fetchFunction, pageSize = 20) {
  let allItems = [];
  let currentPage = 0;
  let hasMorePages = true;
  
  while (hasMorePages) {
    const response = await fetchFunction(currentPage, pageSize);
    
    // Add items from current page
    allItems = [...allItems, ...response.content];
    
    // Check if there are more pages
    hasMorePages = !response.last;
    currentPage++;
  }
  
  return allItems;
}

// Example: Fetch all posts from a user
async function getUserPosts(userId) {
  const fetchPage = (page, size) => 
    fetchWithAuth(`https://api.testwigr.example.com/api/posts/user/${userId}?page=${page}&size=${size}`);
  
  return fetchAllPages(fetchPage);
}

// Example usage
getUserPosts('user123')
  .then(posts => console.log(`Found ${posts.length} posts`))
  .catch(error => console.error('Error fetching posts:', error));
```

#### Example (Python)

```python
def fetch_all_pages(client, endpoint, params=None):
    """Fetch all pages of a paginated endpoint."""
    if params is None:
        params = {}
    
    all_items = []
    current_page = 0
    has_more_pages = True
    
    while has_more_pages:
        page_params = {**params, "page": current_page, "size": 20}
        response = requests.get(
            f"{client.base_url}{endpoint}",
            headers=client._get_headers(),
            params=page_params
        )
        data = response.json()
        
        # Add items from current page
        all_items.extend(data.get("content", []))
        
        # Check if there are more pages
        has_more_pages = not data.get("last", True)
        current_page += 1
    
    return all_items

# Example: Fetch all followers of a user
def get_all_followers(client):
    """Get all followers of the current user."""
    return fetch_all_pages(client, "/api/follow/followers")

# Example usage
followers = get_all_followers(client)
print(f"Total followers: {len(followers)}")
```

## Rate Limiting

### Handling Rate Limit Errors

#### Example (JavaScript)

```javascript
// Rate limit aware fetch function
async function rateLimitAwareFetch(url, options = {}, maxRetries = 3) {
  let retries = 0;
  
  while (retries < maxRetries) {
    try {
      const response = await fetchWithAuth(url, options);
      return response;
    } catch (error) {
      if (error.status === 429) {
        // Rate limit exceeded
        const retryAfter = error.headers?.get('Retry-After') || 5;
        console.log(`Rate limit exceeded. Retrying in ${retryAfter} seconds...`);
        
        // Wait for the specified time before retrying
        await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
        retries++;
      } else {
        // Other error, don't retry
        throw error;
      }
    }
  }
  
  throw new Error('Rate limit retry count exceeded');
}
```

#### Example (Python)

```python
def rate_limit_aware_request(client, method, endpoint, data=None, params=None, max_retries=3):
    """Make a request with automatic rate limit handling."""
    retries = 0
    
    while retries < max_retries:
        try:
            return client._request(method, endpoint, data, params)
        except ApiError as e:
            if e.status_code == 429:
                # Get retry time from headers or default to 5 seconds
                retry_after = int(e.headers.get('Retry-After', 5))
                print(f"Rate limit exceeded. Retrying in {retry_after} seconds...")
                
                # Wait before retrying
                time.sleep(retry_after)
                retries += 1
            else:
                # Other error, don't retry
                raise
    
    raise ApiError(429, "Rate limit retry count exceeded")
```

## Versioning

### Specifying API Version

#### Example (JavaScript)

```javascript
// Function to make version-specific API requests
async function fetchWithVersion(url, version = '1.0.0', options = {}) {
  const token = localStorage.getItem('authToken');
  
  const headers = {
    ...options.headers,
    'Authorization': token ? `Bearer ${token}` : undefined,
    'X-API-Version': version,
  };
  
  return fetch(url, {
    ...options,
    headers,
  });
}

// Example: Use a specific API version
async function getUserProfileV1(userId) {
  const response = await fetchWithVersion(
    `https://api.testwigr.example.com/api/users/${userId}`, 
    '1.0.0'
  );
  return response.json();
}
```

#### Example (Python)

```python
class TestwigrClient:
    def __init__(self, base_url="https://api.testwigr.example.com", api_version="1.0.0"):
        self.base_url = base_url
        self.api_version = api_version
        self.token = None
    
    def _get_headers(self):
        """Get headers including the authentication token and API version."""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-API-Version": self.api_version
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers

# Example: Create client with specific API version
client_v1 = TestwigrClient(api_version="1.0.0")
```

## Best Practices

1. **Authentication**: Store tokens securely, refresh when needed, and implement proper logout.
2. **Error Handling**: Always handle errors gracefully and provide appropriate feedback to users.
3. **Pagination**: Use pagination for all list endpoints to improve performance.
4. **Rate Limiting**: Respect rate limits and implement exponential backoff for retries.
5. **Versioning**: Specify API version in requests to ensure compatibility.
6. **Validation**: Validate input before sending to the API to reduce errors.
7. **Caching**: Implement appropriate caching for frequently accessed resources.

## SDKs and Client Libraries

For a more streamlined experience, consider using one of our official client libraries:

- **JavaScript/TypeScript**: `testwigr-js` ([GitHub](https://github.com/testwigr/testwigr-js))
- **Python**: `testwigr-python` ([GitHub](https://github.com/testwigr/testwigr-python))
- **Java**: `testwigr-java` ([GitHub](https://github.com/testwigr/testwigr-java))
- **Ruby**: `testwigr-ruby` ([GitHub](https://github.com/testwigr/testwigr-ruby))

These libraries handle authentication, error handling, pagination, and rate limiting automatically, allowing you to focus on your application logic.
