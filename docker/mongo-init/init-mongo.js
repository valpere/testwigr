// /docker/mongo-init/init-mongo.js
db = db.getSiblingDB('testwigr');

// Create application user with appropriate permissions
db.createUser({
  user: 'testwigr_app',
  pwd: process.env.MONGO_APP_PASSWORD || 'testwigr_password',
  roles: [
    { role: 'readWrite', db: 'testwigr' }
  ]
});

// Create collections with validation
db.createCollection('users', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['username', 'email', 'password'],
      properties: {
        username: {
          bsonType: 'string',
          description: 'must be a string and is required'
        },
        email: {
          bsonType: 'string',
          description: 'must be a string and is required'
        },
        password: {
          bsonType: 'string',
          description: 'must be a string and is required'
        }
      }
    }
  }
});

db.createCollection('posts');

// Create indexes for performance
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
db.posts.createIndex({ "userId": 1 });
db.posts.createIndex({ "createdAt": -1 });
