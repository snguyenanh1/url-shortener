DELETE FROM url;
DELETE FROM user;

INSERT INTO user (id, username, password, email, name, role)
VALUES (1, 'testuser', '$2a$10$bgvVqNZpqTACAVSbkXPE7eU1tD/KcKFN6pTKWBwN2wtyYTRT1jso6', 'test@example.com', 'Test User', 'USER');

INSERT INTO user (id, username, password, email, name, role)
VALUES (2, 'adminuser', '$2a$10$bgvVqNZpqTACAVSbkXPE7eU1tD/KcKFN6pTKWBwN2wtyYTRT1jso6', 'admin@example.com', 'Admin User', 'ADMIN');

INSERT INTO user (id, username, password, email, name, role)
VALUES (3, 'otheruser', '$2a$10$bgvVqNZpqTACAVSbkXPE7eU1tD/KcKFN6pTKWBwN2wtyYTRT1jso6', 'other@example.com', 'Other User', 'USER');

INSERT INTO user (id, username, password, email, name, role)
VALUES (4, 'adminuser2', '$2a$10$bgvVqNZpqTACAVSbkXPE7eU1tD/KcKFN6pTKWBwN2wtyYTRT1jso6', 'admin2@example.com', 'Admin User 2', 'ADMIN');

INSERT INTO url (url, short_code, created_at, expires_at, clicks, created_by)
VALUES ('https://facebook.com', 'fb123', NOW(), DATEADD(DAY, 7, NOW()), 0, 1);

INSERT INTO url (url, short_code, created_at, expires_at, clicks, created_by)
VALUES ('https://youtube.com', 'yt456', DATEADD(DAY, -10, NOW()), DATEADD(DAY, -1, NOW()), 0, 1);

INSERT INTO url (url, short_code, created_at, expires_at, clicks)
VALUES ('https://github.com', 'gh789', NOW(), DATEADD(DAY, 7, NOW()), 0);
