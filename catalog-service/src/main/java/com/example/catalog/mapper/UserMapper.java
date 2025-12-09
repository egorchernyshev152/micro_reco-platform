package com.example.catalog.mapper;

import com.example.catalog.dto.UserDto;
import com.example.catalog.entity.User;
import com.example.catalog.model.UserModel;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserModel toModel(User entity) {
        if (entity == null) return null;
        return UserModel.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .build();
    }

    public static User toEntity(UserModel model) {
        if (model == null) return null;
        return User.builder()
                .id(model.getId())
                .name(model.getName())
                .email(model.getEmail())
                .build();
    }

    public static UserModel fromDto(UserDto dto) {
        if (dto == null) return null;
        return UserModel.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }

    public static UserDto toDto(UserModel model) {
        if (model == null) return null;
        return UserDto.builder()
                .id(model.getId())
                .name(model.getName())
                .email(model.getEmail())
                .build();
    }
}
