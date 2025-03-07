# Testwigr Deployment Guide

This document provides comprehensive instructions for deploying the Testwigr application in various environments. Whether you're setting up a development environment, running tests, or deploying to production, this guide will walk you through the necessary steps.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Configuration](#environment-configuration)
- [Development Deployment](#development-deployment)
- [Test Environment Setup](#test-environment-setup)
- [Production Deployment](#production-deployment)
- [Monitoring](#monitoring)
- [Backup and Recovery](#backup-and-recovery)
- [Continuous Integration/Deployment](#continuous-integrationdeployment)
- [Scaling](#scaling)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before deploying Testwigr, ensure you have the following installed:

- Docker (20.10.x or higher)
- Docker Compose (2.x or higher)
- JDK 21 or higher (for local development)
- Gradle 8.x (for local development)
- Git

Verify your installations:

```bash
docker --version
docker-compose --version
java --version
gradle --version
git --version
```

## Environment Configuration

Testwigr uses environment variables for configuration. For production deployments, these should be securely managed.

### Environment Variables

Create a `.env` file based on the template below:

```
# MongoDB credentials
MONGO_USER=admin
MONGO_PASSWORD=secure_password_here

# Application database user
MONGO_APP_USER=testwigr_app
MONGO_APP_PASSWORD=secure_app_password_here

# JWT configuration
JWT_SECRET=generate_a_secure_random_string_here
JWT_EXPIRATION=86400000

# Application configuration
SPRING_PROFILES_ACTIVE=prod

# Monitoring credentials (optional)
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=secure_admin_password_here
```

**Important**: Never commit `.env` files to your repository. Use `.env.template` for documentation purposes.

#### Where should the file `.env.template` be?

The `.env.template` file should be placed in the root directory of your project, right alongside where the actual `.env` file would be created. This makes it easily discoverable for developers setting up the project.

The purpose of the `.env.template` file is to document all the environment variables that your application uses, without including any actual sensitive values. When a developer clones your repository, they can copy this template file to create their own `.env` file with appropriate values.

So in your project structure, it would look something like this:

```plaintext
testwigr/
├── docker/
├── scripts/
├── src/
├── build.gradle
├── settings.gradle
├── README.md
├── TESTING.md
├── DEPLOY.md
├── .env.template    <-- Here
└── .gitignore      (should include .env to prevent committing actual values)
```

This location makes sense because:

1. It's at the project root where configuration files are typically expected
2. It's adjacent to where the actual `.env` file will be created
3. It's easily visible to new developers exploring the project
4. The `.env` file it templates will be read from this location by the deployment scripts I included in the DEPLOY.md

The `.gitignore` file should explicitly exclude `.env` but include `.env.template`, ensuring that the template is version controlled while actual environment values remain private and local to each deployment.

### Generate Secure Secrets

For production deployments, generate secure random strings for secrets:

```bash
openssl rand -base64 32  # Use the output as JWT_SECRET
```

## Development Deployment

For local development environments:

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/testwigr.git
   cd testwigr
   ```

2. Start MongoDB using Docker:

   ```bash
   docker-compose -f docker/docker-compose.yml up -d mongodb
   ```

3. Run the application using Gradle:

   ```bash
   ./gradlew bootRun
   ```

Alternatively, run the complete stack using Docker Compose:

```bash
docker-compose -f docker/docker-compose.yml up -d
```

The application will be available at `http://localhost:8080`.

## Test Environment Setup

To set up the test environment:

1. Start the test database:

   ```bash
   ./scripts/start-test-db.sh
   ```

2. Run the tests:

   ```bash
   ./gradlew test
   ```

3. Stop the test database when done:

   ```bash
   ./scripts/stop-test-db.sh
   ```

For continuous testing during development:

```bash
./gradlew test --continuous
```

## Production Deployment

Production deployment utilizes Docker for containerization, NGINX as a reverse proxy, and proper environment isolation.

### Preparation

1. Ensure server requirements are met:
   - Linux server (Ubuntu 20.04 LTS or higher recommended)
   - Docker and Docker Compose installed
   - At least 2GB RAM, 2 CPU cores, and 20GB storage
   - Domain name with DNS configured (for HTTPS)

2. Set up SSL certificates:
   - Place your SSL certificate and key in `docker/nginx/ssl/`
   - Name them `testwigr.crt` and `testwigr.key`
   - For development or testing, you can generate self-signed certificates:

     ```bash
     mkdir -p docker/nginx/ssl
     openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
       -keyout docker/nginx/ssl/testwigr.key \
       -out docker/nginx/ssl/testwigr.crt
     ```

3. Configure domain name:
   - Update `server_name` in `docker/nginx/conf/default.conf` to match your domain

### Deployment Steps

1. Copy the environment template and set secure values:

   ```bash
   cp .env.template .env
   # Edit .env with appropriate values
   ```

2. Create required directories:

   ```bash
   mkdir -p docker/nginx/logs
   mkdir -p docker/mongo-init
   mkdir -p /backups
   ```

3. Deploy the application:

   ```bash
   ./scripts/deploy-production.sh
   ```

   If you don't have the script, run these commands manually:

   ```bash
   # Build the application
   ./gradlew clean build -x test

   # Deploy with Docker Compose
   docker-compose -f docker/docker-compose-prod.yml down
   docker-compose -f docker/docker-compose-prod.yml build
   docker-compose -f docker/docker-compose-prod.yml up -d
   ```

4. Verify the deployment:

   ```bash
   docker ps
   curl -k https://localhost/actuator/health
   ```

### Update Deployment

To update an existing deployment with new code:

1. Pull the latest changes:

   ```bash
   git pull origin main
   ```

2. Rebuild and redeploy:

   ```bash
   ./scripts/deploy-production.sh
   ```

## Monitoring

Testwigr includes a monitoring stack with Prometheus and Grafana.

### Setup Monitoring

1. Start the monitoring services:

   ```bash
   docker-compose -f docker/docker-compose-monitoring.yml up -d
   ```

2. Access the dashboards:
   - Prometheus: <http://your-server-ip:9090>
   - Grafana: <http://your-server-ip:3000> (login with credentials from .env)

3. Import dashboards in Grafana:
   - Navigate to Dashboards > Import
   - Import dashboard ID 4701 for Spring Boot statistics

### Health Checks

The application exposes health endpoints:

- `/actuator/health` - Overall application health
- `/actuator/metrics` - Detailed metrics
- `/actuator/prometheus` - Prometheus-formatted metrics

## Backup and Recovery

Regular backups are essential for data safety.

### Automated Backups

Set up a cron job to run the backup script daily:

```bash
# Add to crontab (crontab -e)
0 2 * * * /path/to/scripts/backup-mongodb.sh >> /var/log/mongodb-backup.log 2>&1
```

The backup script:

- Creates a MongoDB dump
- Compresses it as a tar.gz file
- Stores it in the /backups directory
- Retains backups for 30 days

### Manual Backup

To perform a manual backup:

```bash
./scripts/backup-mongodb.sh
```

### Restore from Backup

To restore from a backup:

1. Stop the application services:

   ```bash
   docker-compose -f docker/docker-compose-prod.yml stop app
   ```

2. Restore the database:

   ```bash
   # Extract the backup
   tar -xzf /backups/testwigr-backup-YYYY-MM-DD_HH-MM-SS.tar.gz -C /tmp

   # Restore to MongoDB
   docker exec -it mongodb-prod mongorestore \
     --username $MONGO_USER \
     --password $MONGO_PASSWORD \
     --authenticationDatabase admin \
     --db testwigr \
     /tmp/testwigr
   ```

3. Restart the application:

   ```bash
   docker-compose -f docker/docker-compose-prod.yml start app
   ```

## Continuous Integration/Deployment

For automated testing and deployment, consider setting up CI/CD with GitHub Actions or Jenkins.

### GitHub Actions Example

Create a file at `.github/workflows/ci-cd.yml`:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Start test MongoDB
        run: ./scripts/start-test-db.sh
      - name: Test with Gradle
        run: ./gradlew test
      - name: Stop test MongoDB
        run: ./scripts/stop-test-db.sh

  deploy:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.HOST }}
          username: ${{ secrets.USERNAME }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /path/to/testwigr
            git pull origin main
            ./scripts/deploy-production.sh
```

## Scaling

For handling increased load, consider these scaling strategies:

### Vertical Scaling

Increase resources for the application container:

```yaml
# In docker-compose-prod.yml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
```

### Horizontal Scaling

For larger deployments, consider:

1. Setting up multiple application instances behind a load balancer
2. Using MongoDB replica sets for database redundancy
3. Implementing Redis for shared session state

## Troubleshooting

### Common Issues and Solutions

1. **Application fails to start**
   - Check logs: `docker-compose -f docker/docker-compose-prod.yml logs app`
   - Verify environment variables: `docker-compose -f docker/docker-compose-prod.yml config`
   - Ensure MongoDB is running: `docker ps | grep mongodb`

2. **MongoDB connection issues**
   - Check MongoDB logs: `docker-compose -f docker/docker-compose-prod.yml logs mongodb`
   - Verify credentials in `.env` file
   - Test connection manually:

     ```bash
     docker exec -it mongodb-prod mongo -u $MONGO_USER -p $MONGO_PASSWORD --authenticationDatabase admin
     ```

3. **NGINX proxy errors**
   - Check NGINX logs: `docker-compose -f docker/docker-compose-prod.yml logs nginx`
   - Verify SSL certificate paths
   - Test direct connection to the application: `curl http://localhost:8080/actuator/health`

4. **JVM memory issues**
   - Increase memory allocation in `docker-compose-prod.yml`
   - Check application logs for OutOfMemoryError
   - Enable GC logging by adding `-XX:+PrintGCDetails -Xloggc:/tmp/gc.log` to JAVA_OPTS

### Viewing Logs

```bash
# View all logs
docker-compose -f docker/docker-compose-prod.yml logs

# View specific service logs
docker-compose -f docker/docker-compose-prod.yml logs app
docker-compose -f docker/docker-compose-prod.yml logs mongodb
docker-compose -f docker/docker-compose-prod.yml logs nginx

# Follow logs in real-time
docker-compose -f docker/docker-compose-prod.yml logs -f app
```

### Support

For additional support, please:

1. Check the project README.md and TESTING.md
2. Review issues in the project repository
3. Contact the development team

---

This deployment guide covers the essentials for running Testwigr in various environments. For project-specific details about the API functionality, please refer to the README.md file.
