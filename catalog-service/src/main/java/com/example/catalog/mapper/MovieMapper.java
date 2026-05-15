package com.example.catalog.mapper;

import com.example.catalog.dto.CastMemberDto;
import com.example.catalog.dto.MovieDto;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.MovieCastMember;
import com.example.catalog.model.CastMemberModel;
import com.example.catalog.model.MovieModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class MovieMapper {
    private MovieMapper() {
    }

    public static MovieModel toModel(Movie entity) {
        if (entity == null) {
            return null;
        }
        return MovieModel.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .originalTitle(entity.getOriginalTitle())
                .originalLanguage(entity.getOriginalLanguage())
                .description(entity.getDescription())
                .synopsis(entity.getSynopsis())
                .releaseYear(entity.getReleaseYear())
                .releaseDate(entity.getReleaseDate())
                .durationMinutes(entity.getDurationMinutes())
                .ageRating(entity.getAgeRating())
                .tagline(entity.getTagline())
                .status(entity.getStatus())
                .posterUrl(entity.getPosterUrl())
                .backdropUrl(entity.getBackdropUrl())
                .trailerUrl(entity.getTrailerUrl())
                .budget(entity.getBudget())
                .revenue(entity.getRevenue())
                .genres(copy(entity.getGenres()))
                .countries(copy(entity.getCountries()))
                .tags(copy(entity.getTags()))
                .averageRating(entity.getAverageRating())
                .ratingsCount(entity.getRatingsCount())
                .importedRating(entity.getImportedRating())
                .cast(toCastModels(entity.getCast()))
                .build();
    }

    public static Movie toEntity(MovieModel model) {
        if (model == null) {
            return null;
        }
        return Movie.builder()
                .id(model.getId())
                .title(model.getTitle())
                .originalTitle(model.getOriginalTitle())
                .originalLanguage(model.getOriginalLanguage())
                .description(model.getDescription())
                .synopsis(model.getSynopsis())
                .releaseYear(model.getReleaseYear())
                .releaseDate(model.getReleaseDate())
                .durationMinutes(model.getDurationMinutes())
                .ageRating(model.getAgeRating())
                .tagline(model.getTagline())
                .status(model.getStatus())
                .posterUrl(model.getPosterUrl())
                .backdropUrl(model.getBackdropUrl())
                .trailerUrl(model.getTrailerUrl())
                .budget(model.getBudget())
                .revenue(model.getRevenue())
                .genres(copy(model.getGenres()))
                .countries(copy(model.getCountries()))
                .tags(copy(model.getTags()))
                .averageRating(model.getAverageRating())
                .ratingsCount(model.getRatingsCount())
                .importedRating(model.getImportedRating())
                .cast(toCastEntities(model.getCast()))
                .build();
    }

    public static MovieModel fromDto(MovieDto dto) {
        if (dto == null) {
            return null;
        }
        return MovieModel.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .originalTitle(dto.getOriginalTitle())
                .originalLanguage(dto.getOriginalLanguage())
                .description(dto.getDescription())
                .synopsis(dto.getSynopsis())
                .releaseYear(dto.getReleaseYear())
                .releaseDate(dto.getReleaseDate())
                .durationMinutes(dto.getDurationMinutes())
                .ageRating(dto.getAgeRating())
                .tagline(dto.getTagline())
                .status(dto.getStatus())
                .posterUrl(dto.getPosterUrl())
                .backdropUrl(dto.getBackdropUrl())
                .trailerUrl(dto.getTrailerUrl())
                .budget(dto.getBudget())
                .revenue(dto.getRevenue())
                .genres(copy(dto.getGenres()))
                .countries(copy(dto.getCountries()))
                .tags(copy(dto.getTags()))
                .averageRating(dto.getAverageRating())
                .ratingsCount(dto.getRatingsCount())
                .importedRating(dto.getImportedRating())
                .cast(fromDtoCast(dto.getCast()))
                .build();
    }

    public static MovieDto toDto(MovieModel model) {
        if (model == null) {
            return null;
        }
        return MovieDto.builder()
                .id(model.getId())
                .title(model.getTitle())
                .originalTitle(model.getOriginalTitle())
                .originalLanguage(model.getOriginalLanguage())
                .description(model.getDescription())
                .synopsis(model.getSynopsis())
                .releaseYear(model.getReleaseYear())
                .releaseDate(model.getReleaseDate())
                .durationMinutes(model.getDurationMinutes())
                .ageRating(model.getAgeRating())
                .tagline(model.getTagline())
                .status(model.getStatus())
                .posterUrl(model.getPosterUrl())
                .backdropUrl(model.getBackdropUrl())
                .trailerUrl(model.getTrailerUrl())
                .budget(model.getBudget())
                .revenue(model.getRevenue())
                .genres(copy(model.getGenres()))
                .countries(copy(model.getCountries()))
                .tags(copy(model.getTags()))
                .averageRating(model.getAverageRating())
                .ratingsCount(model.getRatingsCount())
                .importedRating(model.getImportedRating())
                .cast(toCastDto(model.getCast()))
                .build();
    }

    private static Set<String> copy(Set<String> source) {
        return source == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source);
    }

    private static List<CastMemberModel> toCastModels(List<MovieCastMember> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(entity -> CastMemberModel.builder()
                        .tmdbId(entity.getPersonTmdbId())
                        .name(entity.getName())
                        .character(entity.getCharacter())
                        .profileUrl(entity.getProfileUrl())
                        .orderIndex(entity.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<MovieCastMember> toCastEntities(List<CastMemberModel> models) {
        if (models == null) {
            return List.of();
        }
        return models.stream()
                .map(model -> MovieCastMember.builder()
                        .personTmdbId(model.getTmdbId())
                        .name(model.getName())
                        .character(model.getCharacter())
                        .profileUrl(model.getProfileUrl())
                        .orderIndex(model.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<CastMemberModel> fromDtoCast(List<CastMemberDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(dto -> CastMemberModel.builder()
                        .tmdbId(dto.getTmdbId())
                        .name(dto.getName())
                        .character(dto.getCharacter())
                        .profileUrl(dto.getProfileUrl())
                        .orderIndex(dto.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<CastMemberDto> toCastDto(List<CastMemberModel> models) {
        if (models == null) {
            return List.of();
        }
        return models.stream()
                .map(model -> CastMemberDto.builder()
                        .tmdbId(model.getTmdbId())
                        .name(model.getName())
                        .character(model.getCharacter())
                        .profileUrl(model.getProfileUrl())
                        .orderIndex(model.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }
}
