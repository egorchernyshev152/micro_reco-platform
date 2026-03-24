package com.example.catalog.mapper;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.Rating;
import com.example.catalog.entity.User;
import com.example.catalog.model.RatingModel;

public final class RatingMapper {
    private RatingMapper() {
    }

    public static RatingModel toModel(Rating entity) {
        if (entity == null) return null;
        return RatingModel.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .movieId(entity.getMovie().getId())
                .score(entity.getScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static Rating toEntity(RatingModel model, User user, Movie movie) {
        if (model == null) return null;
        return Rating.builder()
                .id(model.getId())
                .user(user)
                .movie(movie)
                .score(model.getScore())
                .createdAt(model.getCreatedAt())
                .build();
    }

    public static RatingModel fromDto(RatingDto dto) {
        if (dto == null) return null;
        return RatingModel.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .movieId(dto.getMovieId())
                .score(dto.getScore())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    public static RatingDto toDto(RatingModel model) {
        if (model == null) return null;
        return RatingDto.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .movieId(model.getMovieId())
                .score(model.getScore())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
