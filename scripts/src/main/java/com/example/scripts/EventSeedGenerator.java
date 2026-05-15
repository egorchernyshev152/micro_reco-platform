package com.example.scripts;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EventSeedGenerator {

    private static final Logger log = LoggerFactory.getLogger(EventSeedGenerator.class);

    private final GeneratorArgs args;
    private final Random random;

    public EventSeedGenerator(GeneratorArgs args) {
        this.args = args;
        this.random = args.seed() == null ? new Random() : new Random(args.seed());
    }

    public void run() throws Exception {
        try (HikariDataSource catalogDs = buildDataSource(args.catalogDsn());
             HikariDataSource eventsDs = buildDataSource(args.eventsDsn())) {

            List<Long> users = fetchUsers(catalogDs);
            List<Movie> movies = fetchMovies(catalogDs);
            if (users.isEmpty() || movies.isEmpty()) {
                throw new IllegalStateException("Users or movies list is empty. Make sure catalog-db is filled.");
            }
            log.info("Fetched {} users and {} movies", users.size(), movies.size());

            List<EventRow> rows = buildRows(users, movies);
            log.info("Generated {} events", rows.size());

            if (args.dryRun()) {
                log.info("Dry run enabled, skipping insert");
                return;
            }

            insertRows(eventsDs, rows);
            log.info("Events inserted successfully");
        }
    }

    private HikariDataSource buildDataSource(String dsn) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(dsn);
        cfg.setMaximumPoolSize(4);
        return new HikariDataSource(cfg);
    }

    private List<Long> fetchUsers(HikariDataSource ds) throws Exception {
        String sql = """
                SELECT id
                FROM users
                WHERE blocked = FALSE
                ORDER BY id
                LIMIT ?
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, args.maxUsers());
            try (ResultSet rs = st.executeQuery()) {
                List<Long> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(rs.getLong("id"));
                }
                return users;
            }
        }
    }

    private List<Movie> fetchMovies(HikariDataSource ds) throws Exception {
        String sql = """
                SELECT m.id AS id,
                       COALESCE(m.duration_minutes, 110) AS duration,
                       COALESCE(array_agg(distinct mg.genre) FILTER (WHERE mg.genre IS NOT NULL), '{}') AS genres
                FROM movies m
                LEFT JOIN movie_genres mg ON mg.movie_id = m.id
                GROUP BY m.id, m.duration_minutes
                ORDER BY m.release_year DESC NULLS LAST, m.id DESC
                LIMIT ?
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, args.moviesPool());
            try (ResultSet rs = st.executeQuery()) {
                List<Movie> movies = new ArrayList<>();
                while (rs.next()) {
                    long id = rs.getLong("id");
                    int duration = rs.getInt("duration");
                    movies.add(new Movie(id, duration));
                }
                return movies;
            }
        }
    }

    private List<EventRow> buildRows(List<Long> users, List<Movie> movies) {
        List<EventRow> rows = new ArrayList<>();
        Instant now = Instant.now();
        for (Long userId : users) {
            int moviesPerUser = randomInt(args.minMoviesPerUser(), args.maxMoviesPerUser());
            Collections.shuffle(movies, random);
            List<Movie> picked = movies.subList(0, Math.min(moviesPerUser, movies.size()));
            for (Movie movie : picked) {
                rows.addAll(buildSequence(userId, movie, now));
            }
        }
        return rows;
    }

    private List<EventRow> buildSequence(Long userId, Movie movie, Instant now) {
        List<EventRow> rows = new ArrayList<>();
        AggregatedSource src = AggregatedSource.pick(random);
        Instant cursor = now.minus(randomInt(0, args.daysRange()), ChronoUnit.DAYS);
        String session = randomSession();

        rows.add(new EventRow(userId, movie.id(), "VIEW_CARD", session, src.source(), src.device(), json("{\"from\":\"" + src.viewSource() + "\"}"), cursor));
        cursor = cursor.plus(2, ChronoUnit.MINUTES);

        if (random.nextDouble() < 0.7) {
            rows.add(new EventRow(userId, movie.id(), "WATCH_TRAILER", session, src.source(), src.device(), null, cursor));
            cursor = cursor.plus(1, ChronoUnit.MINUTES);
        }

        if (random.nextDouble() < 0.85) {
            rows.add(new EventRow(userId, movie.id(), "START_WATCHING", session, src.source(), src.device(), json("{\"position\":0}"), cursor));
            cursor = cursor.plus(randomInt(40, 140), ChronoUnit.MINUTES);
            rows.add(new EventRow(userId, movie.id(), "FINISH_WATCHING", session, src.source(), src.device(), json("{\"position\":" + movie.duration() + "}"), cursor));
            cursor = cursor.plus(5, ChronoUnit.MINUTES);
            int score = randomInt(6, 10);
            rows.add(new EventRow(userId, movie.id(), "RATE", session, src.source(), src.device(), json("{\"score\":" + score + "}"), cursor));
            if (random.nextDouble() < 0.35) {
                rows.add(new EventRow(userId, movie.id(), "FAVORITE", session, src.source(), src.device(), null, cursor.plus(1, ChronoUnit.MINUTES)));
            }
        } else if (random.nextDouble() < 0.4) {
            rows.add(new EventRow(userId, movie.id(), "BOOKMARK", session, src.source(), src.device(), null, cursor));
        }

        return rows;
    }

    private void insertRows(HikariDataSource eventsDs, List<EventRow> rows) throws Exception {
        String sql = """
                INSERT INTO events (user_id, movie_id, type, session_id, source, device, payload, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """;
        try (Connection conn = eventsDs.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            int batch = 0;
            for (EventRow row : rows) {
                st.setLong(1, row.userId());
                st.setLong(2, row.movieId());
                st.setString(3, row.type());
                st.setString(4, row.sessionId());
                st.setString(5, row.source());
                st.setString(6, row.device());
                st.setString(7, row.payload());
                st.setTimestamp(8, Timestamp.from(row.createdAt()));
                st.addBatch();
                batch++;
                if (batch >= 500) {
                    st.executeBatch();
                    batch = 0;
                }
            }
            if (batch > 0) {
                st.executeBatch();
            }
        }
    }

    private String randomSession() {
        return "sess-" + RandomStringUtils.randomAlphanumeric(6).toLowerCase();
    }

    private int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private String json(String raw) {
        return raw;
    }

    public static void main(String[] args) throws Exception {
        GeneratorArgs parsed = GeneratorArgs.parse(args);
        new EventSeedGenerator(parsed).run();
    }

    private record Movie(long id, int duration) {}

    private record EventRow(Long userId, Long movieId, String type, String sessionId, String source,
                            String device, String payload, Instant createdAt) {
    }

    private record AggregatedSource(String source, String device, String viewSource) {
        private static final AggregatedSource[] SOURCES = {
                new AggregatedSource("CATALOG", "web:chrome", "search"),
                new AggregatedSource("CATALOG", "mobile:ios", "main_banner"),
                new AggregatedSource("RECOMMENDER_HOME", "web:edge", "trending_wall"),
                new AggregatedSource("SIMILAR_WIDGET", "tv:tizen", "similar_widget"),
                new AggregatedSource("FRIENDS_SIMILAR_USERS", "web:firefox", "friends_feed")
        };

        static AggregatedSource pick(Random random) {
            return SOURCES[random.nextInt(SOURCES.length)];
        }
    }
}
