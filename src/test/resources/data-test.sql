DELETE FROM urls;

INSERT INTO urls (url, short_code, created_at, expires_at)
VALUES ('https://facebook.com', 'fb123', NOW(), DATEADD(DAY, 7, NOW()));

INSERT INTO urls (url, short_code, created_at, expires_at)
VALUES ('https://youtube.com', 'yt456', DATEADD(DAY, -10, NOW()), DATEADD(DAY, -1, NOW()));