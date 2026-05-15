package com.example.recommender.service.algorithm;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.dto.MovieDto;
import com.example.recommender.dto.RecommendationItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContentSimilarityService {

    private final CatalogClient catalogClient;

    public ContentScoreResult score(Set<Long> referenceMovieIds,
                                    Long focusMovieId,
                                    Set<Long> excludeIds,
                                    int limit) {
        List<MovieDto> allMovies = catalogClient.getAllMovies();
        Map<Long, MovieDto> byId = allMovies.stream()
                .collect(Collectors.toMap(MovieDto::getId, m -> m));

        List<MovieDto> referenceMovies = new ArrayList<>();
        if (focusMovieId != null && byId.containsKey(focusMovieId)) {
            referenceMovies.add(byId.get(focusMovieId));
        }
        if (referenceMovies.isEmpty() && referenceMovieIds != null) {
            referenceMovieIds.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .forEach(referenceMovies::add);
        }

        if (referenceMovies.isEmpty()) {
            return new ContentScoreResult(List.of(), byId);
        }

        Map<String, Double> featureWeights = new HashMap<>();
        for (MovieDto movie : referenceMovies) {
            accumulateFeatures(featureWeights, movie, 1.0);
        }

        Map<Long, Double> candidateScores = new HashMap<>();
        for (MovieDto candidate : allMovies) {
            if (excludeIds != null && excludeIds.contains(candidate.getId())) {
                continue;
            }
            double score = computeScore(featureWeights, candidate);
            if (score > 0) {
                candidateScores.put(candidate.getId(), score);
            }
        }

        List<RecommendationItemDto> items = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> RecommendationItemDto.builder()
                        .movie(byId.get(entry.getKey()))
                        .score(entry.getValue())
                        .contentScore(entry.getValue())
                        .build())
                .toList();

        return new ContentScoreResult(items, byId);
    }

    private void accumulateFeatures(Map<String, Double> bag, MovieDto movie, double baseWeight) {
        movie.getGenres().forEach(genre -> bag.merge("genre:" + genre.toLowerCase(), baseWeight, Double::sum));
        movie.getCountries().forEach(country -> bag.merge("country:" + country.toLowerCase(), baseWeight * 0.5, Double::sum));
        movie.getTags().forEach(tag -> bag.merge("tag:" + tag.toLowerCase(), baseWeight * 0.7, Double::sum));
        if (movie.getReleaseYear() != null) {
            bag.merge("year:" + (movie.getReleaseYear() / 10), baseWeight * 0.3, Double::sum);
        }
    }

    private double computeScore(Map<String, Double> features, MovieDto candidate) {
        double score = 0.0;
        for (String genre : candidate.getGenres()) {
            score += features.getOrDefault("genre:" + genre.toLowerCase(), 0.0);
        }
        for (String country : candidate.getCountries()) {
            score += features.getOrDefault("country:" + country.toLowerCase(), 0.0);
        }
        for (String tag : candidate.getTags()) {
            score += features.getOrDefault("tag:" + tag.toLowerCase(), 0.0);
        }
        if (candidate.getReleaseYear() != null) {
            score += features.getOrDefault("year:" + (candidate.getReleaseYear() / 10), 0.0);
        }
        return score;
    }

    public record ContentScoreResult(List<RecommendationItemDto> items, Map<Long, MovieDto> movieIndex) {
    }
}
