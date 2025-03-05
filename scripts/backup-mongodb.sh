#!/bin/bash
# /scripts/backup-mongodb.sh

set -e

# Load environment variables
source .env

# Date format for backup files
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_DIR="/backups"

# Ensure backup directory exists
mkdir -p $BACKUP_DIR

# Perform backup
echo "Starting MongoDB backup..."
docker exec mongodb-prod mongodump \
  --username $MONGO_USER \
  --password $MONGO_PASSWORD \
  --authenticationDatabase admin \
  --db testwigr \
  --out /tmp/backup

# Create archive
docker exec mongodb-prod tar -czf /tmp/testwigr-backup-$DATE.tar.gz -C /tmp/backup .

# Copy to host
docker cp mongodb-prod:/tmp/testwigr-backup-$DATE.tar.gz $BACKUP_DIR/

# Cleanup
docker exec mongodb-prod rm -rf /tmp/backup /tmp/testwigr-backup-$DATE.tar.gz

echo "Backup completed: $BACKUP_DIR/testwigr-backup-$DATE.tar.gz"

# Delete backups older than 30 days
find $BACKUP_DIR -name "testwigr-backup-*.tar.gz" -type f -mtime +30 -delete
echo "Deleted backups older than 30 days"
