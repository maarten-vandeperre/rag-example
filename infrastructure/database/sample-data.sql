INSERT INTO users (user_id, username, email, role, created_at, is_active)
VALUES
    ('user-1', 'john_doe', 'john@example.com', 'STANDARD', NOW(), true),
    ('user-2', 'jane_admin', 'jane@example.com', 'ADMIN', NOW(), true);
