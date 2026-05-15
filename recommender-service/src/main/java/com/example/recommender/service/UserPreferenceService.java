package com.example.recommender.service;

import com.example.recommender.dto.MovieDto;
import com.example.recommender.dto.RecommendationItemDto;
import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.dto.UserPreferenceDto;
import com.example.recommender.dto.UserPreferenceRequest;
import com.example.recommender.model.UserRecommendationPreference;
import com.example.recommender.repository.UserRecommendationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Year;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private static final double DEFAULT_VALUE = 0.5;
    private static final double GENRE_BOOST = 0.12;
    private static final double MUTE_PENALTY = 0.35;
    private static final int FRESHNESS_WINDOW_YEARS = 20;

    private final UserRecommendationPreferenceRepository repository;

    public UserPreferenceDto getPreferences(Long userId) {
        return toDto(repository.findById(userId).orElseGet(() -> defaults(userId)));
    }

    public UserPreferenceDto updatePreferences(Long userId, UserPreferenceRequest request) {
        UserRecommendationPreference preference = repository.findById(userId).orElseGet(() -> defaults(userId));
        preference.setBoostGenres(normalizeGenres(request.getBoostGenres()));
        preference.setMuteGenres(normalizeGenres(request.getMuteGenres()));
        preference.setFreshnessBias(clamp(request.getFreshnessBias(), DEFAULT_VALUE));
        preference.setDiscoveryBias(clamp(request.getDiscoveryBias(), DEFAULT_VALUE));
        UserRecommendationPreference saved = repository.save(preference);
        return toDto(saved);
    }

    public void applyUserPreferences(Long userId, RecommendationResponse response) {
        if (userId == null || response == null || CollectionUtils.isEmpty(response.getItems())) {
            return;
        }
        repository.findById(userId).ifPresent(preference -> reRank(response.getItems(), preference));
    }

    private void reRank(List<RecommendationItemDto> items, UserRecommendationPreference preference) {
        Set<String> boost = normalizeGenres(preference.getBoostGenres());
        Set<String> mute = normalizeGenres(preference.getMuteGenres());
        Set<String> boostLookup = toLookup(boost);
        Set<String> muteLookup = toLookup(mute);
        double freshnessDelta = preference.getFreshnessBias() - DEFAULT_VALUE;
        double discoveryDelta = preference.getDiscoveryBias() - DEFAULT_VALUE;

        for (RecommendationItemDto item : items) {
            MovieDto movie = item.getMovie();
            if (movie == null) {
                continue;
            }
            double updated = item.getScore();
            Set<String> movieGenres = movie.getGenres().stream()
                    .map(genre -> genre == null ? null : genre.toLowerCase(Locale.ROOT))
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            long muted = movieGenres.stream().filter(muteLookup::contains).count();
            if (muted > 0) {
                updated *= MUTE_PENALTY;
            } else {
                long boosted = movieGenres.stream().filter(boostLookup::contains).count();
                if (boosted > 0) {
                    updated += GENRE_BOOST * boosted;
                }
            }

            double freshnessScore = evaluateFreshness(movie);
            double surpriseScore = evaluateNovelty(item);
            updated += (freshnessScore - DEFAULT_VALUE) * freshnessDelta;
            updated += (surpriseScore - DEFAULT_VALUE) * discoveryDelta;
            item.setScore(updated);
        }
        items.sort(Comparator.comparingDouble(RecommendationItemDto::getScore).reversed());
    }

    private double evaluateFreshness(MovieDto movie) {
        Integer releaseYear = movie.getReleaseYear();
        if (releaseYear == null) {
            return DEFAULT_VALUE;
        }
        int currentYear = Year.now().getValue();
        int delta = Math.max(0, currentYear - releaseYear);
        if (delta >= FRESHNESS_WINDOW_YEARS) {
            return 0.0;
        }
        return 1.0 - ((double) delta / FRESHNESS_WINDOW_YEARS);
    }

    private double evaluateNovelty(RecommendationItemDto item) {
        Double popularity = item.getPopularityScore();
        if (popularity == null || popularity <= 0.0) {
            return 1.0;
        }
        double normalized = 1.0 / (1.0 + Math.log10(popularity + 1));
        return clamp(normalized, DEFAULT_VALUE);
    }

    private double clamp(Double value, double fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private Set<String> normalizeGenres(Set<String> genres) {
        if (CollectionUtils.isEmpty(genres)) {
            return new LinkedHashSet<>();
        }
        return genres.stream()
                .filter(g -> g != null && !g.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> toLookup(Set<String> genres) {
        return genres.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private UserPreferenceDto toDto(UserRecommendationPreference preference) {
        return UserPreferenceDto.builder()
                .userId(preference.getUserId())
                .boostGenres(new LinkedHashSet<>(preference.getBoostGenres()))
                .muteGenres(new LinkedHashSet<>(preference.getMuteGenres()))
                .freshnessBias(clamp(preference.getFreshnessBias(), DEFAULT_VALUE))
                .discoveryBias(clamp(preference.getDiscoveryBias(), DEFAULT_VALUE))
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private UserRecommendationPreference defaults(Long userId) {
        UserRecommendationPreference preference = new UserRecommendationPreference();
        preference.setUserId(userId);
        preference.setBoostGenres(new LinkedHashSet<>());
        preference.setMuteGenres(new LinkedHashSet<>());
        preference.setFreshnessBias(DEFAULT_VALUE);
        preference.setDiscoveryBias(DEFAULT_VALUE);
        return preference;
    }
}
