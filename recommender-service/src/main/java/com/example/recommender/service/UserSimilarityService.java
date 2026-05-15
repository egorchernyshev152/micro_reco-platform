package com.example.recommender.service;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.client.EventClient;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.SimilarUserDto;
import com.example.recommender.dto.SimilarUsersResponse;
import com.example.recommender.dto.UserProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSimilarityService {

    private static final Map<String, Double> DEFAULT_EVENT_WEIGHTS = Map.of(
            "VIEW_CARD", 1.0,
            "WATCH_TRAILER", 1.5,
            "FAVORITE", 4.0,
            "BOOKMARK", 2.5,
            "SHARE", 2.0,
            "START_WATCHING", 3.0,
            "FINISH_WATCHING", 5.0,
            "RATE", 4.5
    );
    private static final int TARGET_USER_EVENT_LIMIT = 2000;
    private static final int GLOBAL_EVENT_LIMIT = 12000;
    private static final int DEFAULT_MIN_SHARED_MOVIES = 2;
    private static final int MAX_RESULT_LIMIT = 20;
    private static final int SHARED_SAMPLE_LIMIT = 5;
    private static final int HALF_LIFE_DAYS = 30;
    private static final double DECAY_LAMBDA = Math.log(2) / HALF_LIFE_DAYS;

    private final EventClient eventClient;
    private final CatalogClient catalogClient;

    public SimilarUsersResponse findSimilarUsers(Long userId, String period, int limit, Integer minSharedOverride) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        int resultLimit = Math.max(1, Math.min(limit, MAX_RESULT_LIMIT));
        int minShared = minSharedOverride != null && minSharedOverride > 0
                ? minSharedOverride
                : DEFAULT_MIN_SHARED_MOVIES;

        Instant now = Instant.now();
        List<EventDto> userEvents = fetchEvents(userId, period, TARGET_USER_EVENT_LIMIT);
        Map<Long, Double> targetVector = buildVector(userEvents, now);
        double targetNorm = vectorNorm(targetVector);
        if (targetVector.isEmpty() || targetNorm == 0.0) {
            return emptyResponse(userId, period);
        }

        List<EventDto> allEvents = fetchEvents(null, period, GLOBAL_EVENT_LIMIT);
        Map<Long, Map<Long, Double>> otherVectors = buildOtherVectors(allEvents, userId, now);
        if (otherVectors.isEmpty()) {
            return emptyResponse(userId, period);
        }

        List<Candidate> candidates = otherVectors.entrySet().stream()
                .map(entry -> buildCandidate(entry.getKey(), entry.getValue(), targetVector, targetNorm, minShared))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(Candidate::similarity).reversed())
                .toList();

        List<SimilarUserDto> items = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (items.size() >= resultLimit) {
                break;
            }
            Optional<UserProfileDto> profile = catalogClient.getPublicUserProfile(candidate.userId());
            if (profile.isEmpty()) {
                continue;
            }
            items.add(SimilarUserDto.builder()
                    .userId(candidate.userId())
                    .similarity(candidate.similarity())
                    .sharedMovies(candidate.sharedMovies())
                    .sharedMovieIds(candidate.sharedMovieIds())
                    .profile(profile.get())
                    .build());
        }

        return SimilarUsersResponse.builder()
                .userId(userId)
                .period(period)
                .generatedAt(now)
                .items(items)
                .build();
    }

    private Optional<Candidate> buildCandidate(Long candidateUserId,
                                               Map<Long, Double> vector,
                                               Map<Long, Double> targetVector,
                                               double targetNorm,
                                               int minShared) {
        if (vector.isEmpty()) {
            return Optional.empty();
        }
        double candidateNorm = vectorNorm(vector);
        if (candidateNorm == 0.0) {
            return Optional.empty();
        }
        Set<Long> sharedMovieIds = vector.keySet().stream()
                .filter(targetVector::containsKey)
                .collect(Collectors.toSet());
        if (sharedMovieIds.size() < minShared) {
            return Optional.empty();
        }
        double similarity = cosineSimilarity(targetVector, vector, targetNorm, candidateNorm);
        if (Double.isNaN(similarity) || similarity <= 0.0) {
            return Optional.empty();
        }

        List<Long> topShared = sharedMovieIds.stream()
                .sorted(Comparator.<Long>comparingDouble(id ->
                        targetVector.getOrDefault(id, 0.0) * vector.getOrDefault(id, 0.0)).reversed())
                .limit(SHARED_SAMPLE_LIMIT)
                .toList();

        return Optional.of(new Candidate(candidateUserId, similarity, sharedMovieIds.size(), topShared));
    }

    private Map<Long, Double> buildVector(List<EventDto> events, Instant now) {
        Map<Long, Double> vector = new HashMap<>();
        if (CollectionUtils.isEmpty(events)) {
            return vector;
        }
        for (EventDto event : events) {
            if (event.getMovieId() == null) {
                continue;
            }
            double score = scoreEvent(event, now);
            if (score <= 0.0) {
                continue;
            }
            vector.merge(event.getMovieId(), score, Double::sum);
        }
        return vector;
    }

    private Map<Long, Map<Long, Double>> buildOtherVectors(List<EventDto> events, Long excludeUserId, Instant now) {
        Map<Long, Map<Long, Double>> vectors = new HashMap<>();
        if (CollectionUtils.isEmpty(events)) {
            return vectors;
        }
        for (EventDto event : events) {
            if (event.getUserId() == null || event.getMovieId() == null) {
                continue;
            }
            if (excludeUserId != null && excludeUserId.equals(event.getUserId())) {
                continue;
            }
            double score = scoreEvent(event, now);
            if (score <= 0.0) {
                continue;
            }
            vectors.computeIfAbsent(event.getUserId(), id -> new HashMap<>())
                    .merge(event.getMovieId(), score, Double::sum);
        }
        return vectors;
    }

    private double scoreEvent(EventDto event, Instant now) {
        double weight = DEFAULT_EVENT_WEIGHTS.getOrDefault(
                event.getType() == null ? "" : event.getType().toUpperCase(),
                1.0
        );
        if ("RATE".equalsIgnoreCase(event.getType()) && event.getPayload() != null) {
            Object score = event.getPayload().get("score");
            if (score instanceof Number number) {
                weight = Math.max(weight, number.doubleValue());
            }
        }
        Instant created = event.getCreatedAt();
        long ageDays = 0;
        if (created != null) {
            ageDays = Duration.between(created, now).toDays();
            if (ageDays < 0) {
                ageDays = 0;
            }
        }
        double decay = Math.exp(-DECAY_LAMBDA * ageDays);
        return weight * decay;
    }

    private double vectorNorm(Map<Long, Double> vector) {
        double sum = 0.0;
        for (double value : vector.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private double cosineSimilarity(Map<Long, Double> base,
                                    Map<Long, Double> other,
                                    double baseNorm,
                                    double otherNorm) {
        if (baseNorm == 0.0 || otherNorm == 0.0) {
            return 0.0;
        }
        double dot = 0.0;
        for (Map.Entry<Long, Double> entry : base.entrySet()) {
            dot += entry.getValue() * other.getOrDefault(entry.getKey(), 0.0);
        }
        return dot / (baseNorm * otherNorm);
    }

    private List<EventDto> fetchEvents(Long userId, String period, int limit) {
        List<EventDto> events = eventClient.getEvents(userId, null, null, period, limit, null);
        return events == null ? List.of() : events;
    }

    private SimilarUsersResponse emptyResponse(Long userId, String period) {
        return SimilarUsersResponse.builder()
                .userId(userId)
                .period(period)
                .generatedAt(Instant.now())
                .items(List.of())
                .build();
    }

    private record Candidate(Long userId, double similarity, int sharedMovies, List<Long> sharedMovieIds) {
    }
}
