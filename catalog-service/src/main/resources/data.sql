-- Users for demo access (movies/events наполняются импортом)
INSERT INTO users (name, email, password_hash, role)
VALUES
    ('Demo Admin', 'admin@example.com', crypt('admin123', gen_salt('bf')), 'ADMIN'),
    ('Demo User', 'demo@example.com', crypt('demo123', gen_salt('bf')), 'USER'),
    ('Active Critic', 'critic@example.com', crypt('critic123', gen_salt('bf')), 'USER')
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_complaints (target_user_id, reporter_user_id, category, description, status)
VALUES
    ((SELECT id FROM users WHERE email = 'demo@example.com'),
     (SELECT id FROM users WHERE email = 'critic@example.com'),
     'Abuse', 'Пользователь рассылает спам и оскорбления в комментариях.', 'PENDING'),
    ((SELECT id FROM users WHERE email = 'demo@example.com'),
     (SELECT id FROM users WHERE email = 'admin@example.com'),
     'Fraud', 'Попытка выдавать себя за модератора. Требует проверки.', 'REVIEWING'),
    ((SELECT id FROM users WHERE email = 'critic@example.com'),
     (SELECT id FROM users WHERE email = 'demo@example.com'),
     'Spam', 'Дублирует отзывы и рейтинги с разных аккаунтов.', 'RESOLVED')
ON CONFLICT DO NOTHING;

INSERT INTO user_audit_log (target_user_id, action, details, performed_by_id, performed_by_email, performed_by_name)
VALUES
    ((SELECT id FROM users WHERE email = 'demo@example.com'),
     'ROLE_UPDATED', 'Повышен до администратора для модерирования событий.',
     (SELECT id FROM users WHERE email = 'admin@example.com'), 'admin@example.com', 'Demo Admin'),
    ((SELECT id FROM users WHERE email = 'demo@example.com'),
     'BLOCK_UPDATED', 'Разблокирован после проверки отчета службы поддержки.',
     (SELECT id FROM users WHERE email = 'admin@example.com'), 'admin@example.com', 'Demo Admin')
ON CONFLICT DO NOTHING;

SELECT setval('movies_id_seq', GREATEST(1, (SELECT COALESCE(MAX(id), 0) FROM movies)));
SELECT setval('users_id_seq', GREATEST(1, (SELECT COALESCE(MAX(id), 0) FROM users)));
SELECT setval('user_complaints_id_seq', GREATEST(1, (SELECT COALESCE(MAX(id), 0) FROM user_complaints)));
SELECT setval('user_audit_log_id_seq', GREATEST(1, (SELECT COALESCE(MAX(id), 0) FROM user_audit_log)));
