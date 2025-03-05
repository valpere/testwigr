#!/bin/bash
# /scripts/deploy-production.sh

set -e

# Load environment variables if .env file exists
if [ -f .env ]; then
    export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
fi

# Check for required environment variables
if [ -z "$MONGO_USER" ] || [ -z "$MONGO_PASSWORD" ] || [ -z "$JWT_SECRET" ]; then
    echo "Missing required environment variables. Please check .env file."
    exit 1
fi

# Create required directories
mkdir -p docker/nginx/logs
mkdir -p docker/nginx/ssl
mkdir -p docker/mongo-init

# Build the application
./gradlew clean build -x test

# Deploy with Docker Compose
docker-compose -f docker/docker-compose-prod.yml down
docker-compose -f docker/docker-compose-prod.yml build
docker-compose -f docker/docker-compose-prod.yml up -d

# Wait for services to start
echo "Waiting for services to start..."
sleep 10

# Check if services are running
if docker ps | grep -q "testwigr-app-prod"; then
    echo "Deployment successful! Application is running."
else
    echo "Deployment failed. Check logs for details."
    docker-compose -f docker/docker-compose-prod.yml logs
    exit 1
fi

echo "Application is accessible at: https://testwigr.example.com"
