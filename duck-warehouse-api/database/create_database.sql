-- Run as a database administrator when not using Docker Compose.
CREATE DATABASE IF NOT EXISTS duck_warehouse
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
-- Create a dedicated local application user and grant privileges on this
-- database using your own password. Never commit actual credentials.
-- Flyway creates the table when the application starts.
