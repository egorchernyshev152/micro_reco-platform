-- Demo dataset for catalog-service
-- WARNING: this script truncates catalog tables. Run only against local/demo DB.
BEGIN;

TRUNCATE TABLE movie_ratings,
    user_movie_collections,
    movie_cast,
    movie_tags,
    movie_countries,
    movie_genres,
    movies RESTART IDENTITY CASCADE;

TRUNCATE TABLE users RESTART IDENTITY CASCADE;

INSERT INTO users (id, name, email, password_hash, role, blocked, profile_private, created_at, updated_at)
VALUES
    (1, 'Demo Admin', 'admin@example.com', crypt('admin123', gen_salt('bf')), 'ADMIN', FALSE, FALSE, NOW(), NOW()),
    (2, 'Demo User', 'demo@example.com', crypt('demo123', gen_salt('bf')), 'USER', FALSE, FALSE, NOW(), NOW()),
    (3, 'Active Critic', 'critic@example.com', crypt('critic123', gen_salt('bf')), 'USER', FALSE, FALSE, NOW(), NOW()),
    (4, 'Гик по фантастике', 'geek@example.com', crypt('geek123', gen_salt('bf')), 'USER', FALSE, FALSE, NOW(), NOW()),
    (5, 'Любитель арт-хауса', 'arthouse@example.com', crypt('art12345', gen_salt('bf')), 'USER', FALSE, FALSE, NOW(), NOW()),
    (6, 'Новичок без истории', 'newbie@example.com', crypt('newbie123', gen_salt('bf')), 'USER', FALSE, FALSE, NOW(), NOW()),
    (7, 'Документалист', 'docu@example.com', crypt('docu1234', gen_salt('bf')), 'USER', FALSE, TRUE, NOW(), NOW());

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

INSERT INTO movies (id, title, original_title, original_language, description, synopsis, release_year, release_date,
                    duration_minutes, age_rating, tagline, status, poster_url, backdrop_url, trailer_url,
                    budget, revenue, average_rating, ratings_count, created_at, updated_at)
VALUES
    (1001, 'Полярный рассвет', 'Polar Dawn', 'ru',
     'Арктическая экспедиция сталкивается с загадочным явлением и вынуждена сплотиться, чтобы выжить.',
     'Команда молодых исследователей приезжает на научную станцию на Новой Земле. Они пытаются спасти архивные данные, пока циклон не уничтожил базу, и открывают новые слои истории семьи главной героини.',
     2022, '2022-11-18', 118, '16+', 'Пока горит северное сияние', 'PUBLISHED',
     'https://demo.microreco/img/polar-dawn.jpg', 'https://demo.microreco/img/polar-dawn-bg.jpg', 'https://demo.microreco/trailers/polar-dawn',
     12000000, 45000000, 8.1, 1250, NOW(), NOW()),
    (1002, 'Звёздный курьер', 'Star Courier', 'en',
     'Космический гонщик доставляет секретный груз через запретные сектора и знакомится с подпольными учёными.',
     'Действие разворачивается в 2145 году. Курьер Миранда принимает заказ на доставку квантового ядра, которое может остановить энергетический кризис Земли. За ней охотятся пираты и корпорации.',
     2024, '2024-03-07', 132, '12+', 'Скорость выше света', 'PUBLISHED',
     'https://demo.microreco/img/star-courier.jpg', 'https://demo.microreco/img/star-courier-bg.jpg', 'https://demo.microreco/trailers/star-courier',
     85000000, 210000000, 7.8, 980, NOW(), NOW()),
    (1003, 'Тихие улицы Киото', 'Silent Streets of Kyoto', 'ja',
     'История художницы из Киото, которая открывает семейную чайную и влюбляется в фотографа из Владивостока.',
     'Фильм балансирует между семейной драмой и романтическим медленным кино. Героиня мчится между обязанностями перед семьёй и желанием создать собственную галерею.', 
     2021, '2021-05-14', 105, '12+', 'Каждый запах — воспоминание', 'PUBLISHED',
     'https://demo.microreco/img/kyoto.jpg', 'https://demo.microreco/img/kyoto-bg.jpg', 'https://demo.microreco/trailers/kyoto',
     6000000, 22000000, 8.6, 870, NOW(), NOW()),
    (1004, 'Берлинский джаз', 'Berlin Jazz Nights', 'de',
     'Музыкальный коллектив беженцев из разных стран собирается в Берлине и записывает альбом, который меняет их жизнь.',
     'Картина соединяет элементы докудрамы и музыкального фильма. Каждый участник коллектива рассказывает свою историю через музыку и ночные концерты.', 
     2020, '2020-09-25', 110, '16+', 'Город звучит даже в тишине', 'PUBLISHED',
     'https://demo.microreco/img/berlin-jazz.jpg', 'https://demo.microreco/img/berlin-jazz-bg.jpg', 'https://demo.microreco/trailers/berlin-jazz',
     9000000, 18000000, 8.4, 640, NOW(), NOW()),
    (1005, 'Алматинский киберспорт', 'Almaty eSports', 'kk',
     'Комедия о казахстанской школьнице, которая собирает команду для международного турнира и борется с консервативными родителями.',
     'Фильм показывает Алма-Ату глазами подростков: тренировки в антикафе, ночные катания и финальный матч против фаворитов из Сеула.', 
     2023, '2023-08-31', 102, '12+', 'GG, если веришь в команду', 'PUBLISHED',
     'https://demo.microreco/img/almaty-esports.jpg', 'https://demo.microreco/img/almaty-esports-bg.jpg', 'https://demo.microreco/trailers/almaty-esports',
     4500000, 9500000, 7.4, 420, NOW(), NOW()),
    (1006, 'Северный ветер', 'Nordic Wind', 'no',
     'Скандинавский триллер о следователе из Норвегии, который расследует исчезновения альпинистов на границе со Швецией.',
     'Из-за метели герои застревают в маленьком городке, где каждый скрывает прошлое. Атмосферный нуар с элементами мистики.', 
     2019, '2019-02-08', 124, '18+', 'Лёд помнит всё', 'PUBLISHED',
     'https://demo.microreco/img/nordic-wind.jpg', 'https://demo.microreco/img/nordic-wind-bg.jpg', 'https://demo.microreco/trailers/nordic-wind',
     15000000, 61000000, 7.9, 1340, NOW(), NOW()),
    (1007, 'Метро 24', 'Metro 24', 'ru',
     'Энсабльный боевик о московском машинисте, который оказывается втянутым в операцию спасения после кибератаки.',
     'Сюжет развивается в течение двадцати четырёх часов. Камеры наблюдения, новости и сообщения пассажиров складываются в одно расследование.', 
     2024, '2024-01-19', 115, '16+', 'Город не спит, пока мы на рельсах', 'PUBLISHED',
     'https://demo.microreco/img/metro24.jpg', 'https://demo.microreco/img/metro24-bg.jpg', 'https://demo.microreco/trailers/metro24',
     20000000, 73000000, 7.2, 510, NOW(), NOW()),
    (1008, 'Письма из Буэнос-Айреса', 'Letters from Buenos Aires', 'es',
     'Молодой шеф-повар из Испании возвращается в Аргентину по письмам погибшего деда и открывает семейное кафе.',
     'Фильм насыщен танго, рецептами и пейзажами Ла-Боки. Это история о принятии культурного наследия и поиске любви.', 
     2022, '2022-06-10', 108, '12+', 'Там, где звучит бандонеон', 'PUBLISHED',
     'https://demo.microreco/img/buenos-aires.jpg', 'https://demo.microreco/img/buenos-aires-bg.jpg', 'https://demo.microreco/trailers/buenos-aires',
     8000000, 24000000, 8.0, 730, NOW(), NOW()),
    (1009, 'Город стекла', 'Glass City', 'en',
     'Киберпанковский триллер про журналистку, раскрывающую тайну корпорации, контролирующей город через AR-очки.',
     'Неоновые улицы Чикаго образуют лабиринт, где каждая реклама может быть ловушкой. Героям помогают подпольные разработчики из Монреаля.', 
     2023, '2023-10-05', 127, '16+', 'Не верь собственным глазам', 'PUBLISHED',
     'https://demo.microreco/img/glass-city.jpg', 'https://demo.microreco/img/glass-city-bg.jpg', 'https://demo.microreco/trailers/glass-city',
     68000000, 198000000, 7.7, 1530, NOW(), NOW()),
    (1010, 'Кавказский драйв', 'Caucasus Drive', 'ka',
     'Дорожная комедия о двух братьях из Тбилиси, которые вынуждены отвезти ретро-машину в Баку, пока за ними гонятся коллекционеры.',
     'Дорога проходит через Грузию, Армению и Азербайджан, а братья встречают своих родственников по всему Кавказу.', 
     2021, '2021-07-30', 99, '12+', 'Главное — не заглохнуть на перевале', 'PUBLISHED',
     'https://demo.microreco/img/caucasus-drive.jpg', 'https://demo.microreco/img/caucasus-drive-bg.jpg', 'https://demo.microreco/trailers/caucasus-drive',
     3500000, 12000000, 7.1, 360, NOW(), NOW()),
    (1011, 'Хроники моря Сахалина', 'Sakhalin Sea Chronicles', 'ru',
     'Документальный фильм о рыбаках Сахалина, японских архивах и редких морских видах.',
     'Картина совмещает интервью с документами послевоенного периода. Музыку написал японский композитор Рюичи Хиромото.', 
     2020, '2020-12-12', 93, '0+', 'Море делится историями', 'PUBLISHED',
     'https://demo.microreco/img/sakhalin.jpg', 'https://demo.microreco/img/sakhalin-bg.jpg', 'https://demo.microreco/trailers/sakhalin',
     2000000, 5200000, 8.3, 410, NOW(), NOW()),
    (1012, 'Самарканд. Золото ветров', 'Samarkand: Gold of Winds', 'uz',
     'Историческая драма о ремесленнице, которая создаёт уникальные ткани во времена Тимура.',
     'Фильм снят в исторических интерьерах, сочетает элементы сказания и семейной саги.', 
     2018, '2018-04-21', 121, '12+', 'Легенда ткётся сотнями нитей', 'PUBLISHED',
     'https://demo.microreco/img/samarkand.jpg', 'https://demo.microreco/img/samarkand-bg.jpg', 'https://demo.microreco/trailers/samarkand',
     10000000, 27000000, 8.5, 690, NOW(), NOW());

SELECT setval('movies_id_seq', (SELECT MAX(id) FROM movies));

-- Genres
INSERT INTO movie_genres (movie_id, genre) VALUES
    (1001, 'Драма'), (1001, 'Приключения'),
    (1002, 'Фантастика'), (1002, 'Боевик'),
    (1003, 'Драма'), (1003, 'Романтика'),
    (1004, 'Музыка'), (1004, 'Драма'),
    (1005, 'Комедия'), (1005, 'Спорт'),
    (1006, 'Триллер'), (1006, 'Детектив'),
    (1007, 'Боевик'), (1007, 'Триллер'),
    (1008, 'Романтика'), (1008, 'Драма'),
    (1009, 'Фантастика'), (1009, 'Триллер'),
    (1010, 'Комедия'), (1010, 'Дорожное кино'),
    (1011, 'Документальный'), (1012, 'Исторический'), (1012, 'Драма')
ON CONFLICT DO NOTHING;

-- Countries
INSERT INTO movie_countries (movie_id, country) VALUES
    (1001, 'Россия'), (1001, 'Финляндия'),
    (1002, 'США'),
    (1003, 'Япония'),
    (1004, 'Германия'),
    (1005, 'Казахстан'),
    (1006, 'Норвегия'), (1006, 'Швеция'),
    (1007, 'Россия'),
    (1008, 'Аргентина'), (1008, 'Испания'),
    (1009, 'США'), (1009, 'Канада'),
    (1010, 'Грузия'), (1010, 'Армения'),
    (1011, 'Россия'), (1011, 'Япония'),
    (1012, 'Узбекистан')
ON CONFLICT DO NOTHING;

-- Tags
INSERT INTO movie_tags (movie_id, tag) VALUES
    (1001, 'арктика'), (1001, 'экспедиция'), (1001, 'семейная драма'),
    (1002, 'космос'), (1002, 'гонки'), (1002, 'киберпанк'),
    (1003, 'чайные традиции'), (1003, 'искусство'), (1003, 'межкультурная любовь'),
    (1004, 'джаз'), (1004, 'эмиграция'),
    (1005, 'киберспорт'), (1005, 'подростки'),
    (1006, 'нуар'), (1006, 'скандинавия'),
    (1007, 'метро'), (1007, 'кибератака'),
    (1008, 'танго'), (1008, 'кулинария'),
    (1009, 'AR'), (1009, 'корпорации'),
    (1010, 'roadtrip'), (1010, 'кавказ'),
    (1011, 'море'), (1011, 'архив'),
    (1012, 'история'), (1012, 'текстиль')
ON CONFLICT DO NOTHING;

-- Small cast samples
INSERT INTO movie_cast (movie_id, order_index, person_tmdb_id, name, character, profile_url)
VALUES
    (1001, 1, NULL, 'Анна Котова', 'Елена Ветрова', NULL),
    (1001, 2, NULL, 'Юсси Лехтинен', 'Доктор Ларс', NULL),
    (1002, 1, NULL, 'Mia Torres', 'Миранда Чоу', NULL),
    (1002, 2, NULL, 'Джон Бейкер', 'Капитан Дрейк', NULL),
    (1003, 1, NULL, 'Сакура Мори', 'Аканэ', NULL),
    (1004, 1, NULL, 'Леонид Сафронов', 'Пианист Данило', NULL),
    (1004, 2, NULL, 'Marta Weiss', 'Вокалистка Элла', NULL),
    (1005, 1, NULL, 'Айя Есимова', 'Алия', NULL),
    (1006, 1, NULL, 'Андерс Лунд', 'Следователь Торстен', NULL),
    (1007, 1, NULL, 'Павел Журавлёв', 'Андрей Плотников', NULL),
    (1008, 1, NULL, 'София Бланко', 'Лусия', NULL),
    (1009, 1, NULL, 'Лилит Чжан', 'Майя Вега', NULL),
    (1010, 1, NULL, 'Гела Ноцеби', 'Нико', NULL),
    (1011, 1, NULL, 'Тадаши Окада', 'Нарратор', NULL),
    (1012, 1, NULL, 'Дильноз Камалова', 'Ширин', NULL)
ON CONFLICT DO NOTHING;

-- Collections
INSERT INTO user_movie_collections (user_id, movie_id, type, created_at)
VALUES
    (2, 1001, 'FAVORITE', NOW()),
    (2, 1002, 'WATCHLIST', NOW()),
    (2, 1009, 'WATCHLIST', NOW()),
    (3, 1004, 'FAVORITE', NOW()),
    (3, 1012, 'WATCHLIST', NOW()),
    (4, 1002, 'FAVORITE', NOW()),
    (4, 1009, 'FAVORITE', NOW()),
    (5, 1003, 'FAVORITE', NOW()),
    (5, 1008, 'FAVORITE', NOW()),
    (7, 1011, 'FAVORITE', NOW())
ON CONFLICT DO NOTHING;

-- Ratings
INSERT INTO movie_ratings (user_id, movie_id, score, created_at)
VALUES
    (2, 1001, 9, '2024-02-12 10:00:00+00'),
    (2, 1002, 8, '2024-02-15 12:00:00+00'),
    (2, 1007, 7, '2024-03-01 09:00:00+00'),
    (3, 1001, 8, '2024-02-20 15:30:00+00'),
    (3, 1004, 9, '2024-01-22 19:00:00+00'),
    (3, 1012, 10, '2024-03-05 14:00:00+00'),
    (4, 1002, 9, '2024-03-10 18:00:00+00'),
    (4, 1009, 8, '2024-03-11 18:30:00+00'),
    (4, 1006, 7, '2024-02-05 21:00:00+00'),
    (5, 1003, 10, '2024-02-18 17:45:00+00'),
    (5, 1008, 9, '2024-02-28 11:20:00+00'),
    (5, 1012, 8, '2024-01-30 13:00:00+00'),
    (7, 1011, 9, '2024-03-02 08:10:00+00'),
    (7, 1008, 7, '2024-03-03 09:25:00+00')
ON CONFLICT ON CONSTRAINT uq_rating_user_movie DO NOTHING;

SELECT setval('movie_ratings_id_seq', (SELECT MAX(id) FROM movie_ratings));
SELECT setval('user_movie_collections_id_seq', (SELECT MAX(id) FROM user_movie_collections));

COMMIT;
