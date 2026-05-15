package com.example.scripts;

import java.util.HashMap;
import java.util.Map;

public record GeneratorArgs(
        String catalogDsn,
        String eventsDsn,
        int maxUsers,
        int moviesPool,
        int minMoviesPerUser,
        int maxMoviesPerUser,
        int daysRange,
        Long seed,
        boolean dryRun
) {
    static GeneratorArgs parse(String[] rawArgs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < rawArgs.length; i++) {
            String arg = rawArgs[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                String value = (i + 1 < rawArgs.length && !rawArgs[i + 1].startsWith("--")) ? rawArgs[++i] : "true";
                map.put(key, value);
            }
        }
        String catalogDsn = map.getOrDefault("catalog-dsn", "jdbc:postgresql://localhost:5433/catalog-db?user=postgres&password=postgres");
        String eventsDsn = map.getOrDefault("events-dsn", "jdbc:postgresql://localhost:5433/event-db?user=postgres&password=postgres");
        int users = Integer.parseInt(map.getOrDefault("users", "40"));
        int moviesPool = Integer.parseInt(map.getOrDefault("movies", "800"));
        int minMovies = Integer.parseInt(map.getOrDefault("min-movies-per-user", "6"));
        int maxMovies = Integer.parseInt(map.getOrDefault("max-movies-per-user", "18"));
        int daysRange = Integer.parseInt(map.getOrDefault("days-range", "60"));
        Long seed = map.containsKey("seed") ? Long.parseLong(map.get("seed")) : null;
        boolean dryRun = Boolean.parseBoolean(map.getOrDefault("dry-run", "false"));
        return new GeneratorArgs(catalogDsn, eventsDsn, users, moviesPool, minMovies, maxMovies, daysRange, seed, dryRun);
    }
}
