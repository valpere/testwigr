# Testwigr - Twitter-like API

A Twitter-like API built with Groovy, Spring Boot, and MongoDB. This project implements core Twitter functionality including user registration, posting, liking, commenting, and following other users.

## Features

- **User Management**
  - Registration and authentication
  - JWT-based authentication
  - Profile management (edit, delete)
  - Follow/unfollow users

- **Post Management**
  - Create, edit, and delete posts
  - View posts from specific users
  - Personal feed (posts from followed users)

- **Social Interactions**
  - Like/unlike posts
  - Comment on posts
  - View post statistics (likes, comments)

## Tech Stack

- **Backend**
  - Groovy
  - Spring Boot 3.4.3
  - Spring Security
  - MongoDB
  - JWT for authentication

- **Build & Deployment**
  - Gradle
  - Docker
  - Docker Compose

- **Testing**
  - Spock Framework

## Getting Started

### Prerequisites

- JDK 21 or higher
- Docker and Docker Compose
- MongoDB (or use the Docker Compose configuration)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/testwigr.git
   cd testwigr
