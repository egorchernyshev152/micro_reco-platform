-- Demo dataset for event-service
-- WARNING: truncates the events table.
BEGIN;

TRUNCATE TABLE events RESTART IDENTITY;

INSERT INTO events (user_id, movie_id, type, session_id, source, device, payload, created_at) VALUES
    (2, 1001, 'VIEW_CARD', 'web-100', 'CATALOG', 'web:chrome', '{"from":"main_banner"}', '2024-02-10 10:00:00+00'),
    (2, 1001, 'WATCH_TRAILER', 'web-100', 'CATALOG', 'web:chrome', '{"duration":120}', '2024-02-10 10:02:00+00'),
    (2, 1001, 'START_WATCHING', 'tv-501', 'CATALOG', 'tv:tizen', '{"position":0}', '2024-02-11 19:30:00+00'),
    (2, 1001, 'FINISH_WATCHING', 'tv-501', 'CATALOG', 'tv:tizen', '{"position":118}', '2024-02-11 21:30:00+00'),
    (2, 1001, 'RATE', 'web-101', 'CATALOG', 'web:chrome', '{"score":9}', '2024-02-12 08:15:00+00'),
    (2, 1002, 'VIEW_CARD', 'web-102', 'RECOMMENDER_HOME', 'web:firefox', '{"widget":"Hero"}', '2024-02-15 09:20:00+00'),
    (2, 1002, 'START_WATCHING', 'mobile-201', 'RECOMMENDER_HOME', 'mobile:ios', '{"position":0}', '2024-02-16 07:00:00+00'),
    (2, 1002, 'FINISH_WATCHING', 'mobile-201', 'RECOMMENDER_HOME', 'mobile:ios', '{"position":132}', '2024-02-16 09:20:00+00'),
    (2, 1002, 'RATE', 'mobile-201', 'RECOMMENDER_HOME', 'mobile:ios', '{"score":8}', '2024-02-16 09:25:00+00'),
    (2, 1007, 'VIEW_CARD', 'web-103', 'SIMILAR_WIDGET', 'web:chrome', '{"fromMovie":1002}', '2024-02-27 12:00:00+00'),
    (2, 1007, 'FAVORITE', 'web-103', 'SIMILAR_WIDGET', 'web:chrome', NULL, '2024-03-01 08:00:00+00'),
    (2, 1009, 'VIEW_CARD', 'web-200', 'RECOMMENDER_HOME', 'web:edge', '{"widget":"TrendingSciFi"}', '2024-03-10 16:00:00+00'),

    (3, 1001, 'VIEW_CARD', 'critic-01', 'CATALOG', 'web:safari', '{"from":"search"}', '2024-02-12 11:00:00+00'),
    (3, 1001, 'RATE', 'critic-01', 'CATALOG', 'web:safari', '{"score":8}', '2024-02-13 11:05:00+00'),
    (3, 1004, 'VIEW_CARD', 'critic-02', 'CATALOG', 'web:safari', '{"from":"editor_choice"}', '2024-01-20 18:00:00+00'),
    (3, 1004, 'WATCH_TRAILER', 'critic-02', 'CATALOG', 'web:safari', NULL, '2024-01-20 18:03:00+00'),
    (3, 1004, 'START_WATCHING', 'critic-02', 'CATALOG', 'tv:webos', '{"position":0}', '2024-01-21 20:00:00+00'),
    (3, 1004, 'FINISH_WATCHING', 'critic-02', 'CATALOG', 'tv:webos', '{"position":110}', '2024-01-21 21:50:00+00'),
    (3, 1004, 'RATE', 'critic-02', 'CATALOG', 'web:safari', '{"score":9}', '2024-01-22 08:00:00+00'),
    (3, 1009, 'VIEW_CARD', 'critic-03', 'RECOMMENDER_HOME', 'web:safari', '{"widget":"CriticPick"}', '2024-03-05 09:00:00+00'),
    (3, 1012, 'VIEW_CARD', 'critic-04', 'CATALOG', 'web:safari', '{"from":"search"}', '2024-02-28 11:15:00+00'),
    (3, 1012, 'START_WATCHING', 'critic-04', 'CATALOG', 'tv:webos', '{"position":0}', '2024-02-28 20:00:00+00'),
    (3, 1012, 'FINISH_WATCHING', 'critic-04', 'CATALOG', 'tv:webos', '{"position":121}', '2024-02-28 22:10:00+00'),
    (3, 1012, 'RATE', 'critic-04', 'CATALOG', 'web:safari', '{"score":10}', '2024-02-29 07:45:00+00'),

    (4, 1002, 'VIEW_CARD', 'geek-01', 'CATALOG', 'mobile:android', '{"from":"push"}', '2024-02-18 13:00:00+00'),
    (4, 1002, 'RATE', 'geek-01', 'CATALOG', 'mobile:android', '{"score":9}', '2024-02-18 18:00:00+00'),
    (4, 1006, 'VIEW_CARD', 'geek-02', 'SIMILAR_WIDGET', 'web:chrome', '{"fromMovie":1002}', '2024-02-22 21:10:00+00'),
    (4, 1006, 'START_WATCHING', 'geek-02', 'SIMILAR_WIDGET', 'tv:tizen', '{"position":0}', '2024-02-22 21:30:00+00'),
    (4, 1006, 'FINISH_WATCHING', 'geek-02', 'SIMILAR_WIDGET', 'tv:tizen', '{"position":124}', '2024-02-22 23:40:00+00'),
    (4, 1006, 'RATE', 'geek-02', 'SIMILAR_WIDGET', 'web:chrome', '{"score":7}', '2024-02-23 08:10:00+00'),
    (4, 1007, 'VIEW_CARD', 'geek-03', 'RECOMMENDER_HOME', 'web:chrome', '{"widget":"MetroSpecial"}', '2024-03-03 12:00:00+00'),
    (4, 1009, 'VIEW_CARD', 'geek-04', 'RECOMMENDER_HOME', 'web:chrome', '{"widget":"SciFiWall"}', '2024-03-05 19:00:00+00'),
    (4, 1009, 'SHARE', 'geek-04', 'RECOMMENDER_HOME', 'web:chrome', '{"target":"friends"}', '2024-03-05 19:02:00+00'),

    (5, 1003, 'VIEW_CARD', 'art-01', 'CATALOG', 'web:safari', '{"from":"curated"}', '2024-02-14 15:00:00+00'),
    (5, 1003, 'START_WATCHING', 'art-01', 'CATALOG', 'tv:android', '{"position":0}', '2024-02-14 22:00:00+00'),
    (5, 1003, 'FINISH_WATCHING', 'art-01', 'CATALOG', 'tv:android', '{"position":105}', '2024-02-15 00:00:00+00'),
    (5, 1003, 'RATE', 'art-01', 'CATALOG', 'web:safari', '{"score":10}', '2024-02-15 08:00:00+00'),
    (5, 1008, 'VIEW_CARD', 'art-02', 'CATALOG', 'mobile:ios', '{"from":"newsletter"}', '2024-02-26 09:00:00+00'),
    (5, 1008, 'FAVORITE', 'art-02', 'CATALOG', 'mobile:ios', NULL, '2024-02-27 12:10:00+00'),
    (5, 1012, 'VIEW_CARD', 'art-03', 'SIMILAR_WIDGET', 'web:safari', '{"fromMovie":1003}', '2024-03-04 18:00:00+00'),
    (5, 1012, 'WATCH_TRAILER', 'art-03', 'SIMILAR_WIDGET', 'web:safari', NULL, '2024-03-04 18:03:00+00'),

    (7, 1011, 'VIEW_CARD', 'docu-01', 'CATALOG', 'web:firefox', '{"from":"documentary"}', '2024-02-25 06:30:00+00'),
    (7, 1011, 'START_WATCHING', 'docu-01', 'CATALOG', 'tv:tizen', '{"position":0}', '2024-02-25 07:00:00+00'),
    (7, 1011, 'FINISH_WATCHING', 'docu-01', 'CATALOG', 'tv:tizen', '{"position":93}', '2024-02-25 08:40:00+00'),
    (7, 1011, 'RATE', 'docu-01', 'CATALOG', 'web:firefox', '{"score":9}', '2024-02-25 09:00:00+00'),
    (7, 1008, 'VIEW_CARD', 'docu-02', 'FRIENDS_SIMILAR_USERS', 'web:firefox', '{"fromUser":5}', '2024-03-06 13:00:00+00'),
    (7, 1008, 'START_WATCHING', 'docu-02', 'FRIENDS_SIMILAR_USERS', 'mobile:android', '{"position":0}', '2024-03-06 21:00:00+00'),
    (7, 1008, 'FINISH_WATCHING', 'docu-02', 'FRIENDS_SIMILAR_USERS', 'mobile:android', '{"position":108}', '2024-03-06 23:00:00+00'),
    (7, 1008, 'RATE', 'docu-02', 'FRIENDS_SIMILAR_USERS', 'web:firefox', '{"score":7}', '2024-03-07 07:45:00+00');

COMMIT;
