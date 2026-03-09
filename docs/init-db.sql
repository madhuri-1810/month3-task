-- Create separate databases for each microservice (Database per Service pattern)

CREATE DATABASE user_db;
CREATE DATABASE post_db;
CREATE DATABASE notification_db;
CREATE DATABASE chat_db;
CREATE DATABASE media_db;
CREATE DATABASE analytics_db;

-- Grant all privileges to admin user
GRANT ALL PRIVILEGES ON DATABASE user_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE post_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE chat_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE media_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO admin;
