package com.example.catalog.mapper;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.entity.Item;
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
                .itemId(entity.getItem().getId())
                .score(entity.getScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static Rating toEntity(RatingModel model, User user, Item item) {
        if (model == null) return null;
        return Rating.builder()
                .id(model.getId())
                .user(user)
                .item(item)
                .score(model.getScore())
                .createdAt(model.getCreatedAt())
                .build();
    }

    public static RatingModel fromDto(RatingDto dto) {
        if (dto == null) return null;
        return RatingModel.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .itemId(dto.getItemId())
                .score(dto.getScore())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    public static RatingDto toDto(RatingModel model) {
        if (model == null) return null;
        return RatingDto.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .itemId(model.getItemId())
                .score(model.getScore())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
