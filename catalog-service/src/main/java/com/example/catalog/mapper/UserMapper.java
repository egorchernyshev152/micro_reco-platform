package com.example.catalog.mapper;

import com.example.catalog.dto.UserDto;
import com.example.catalog.entity.User;
import com.example.catalog.model.UserModel;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserModel toModel(User entity) {
        if (entity == null) {
            return null;
        }
        return UserModel.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .blocked(entity.isBlocked())
                .profilePrivate(entity.isProfilePrivate())
                .roleChangedAt(entity.getRoleChangedAt())
                .roleChangedBy(entity.getRoleChangedBy())
                .blockedChangedAt(entity.getBlockedChangedAt())
                .blockedChangedBy(entity.getBlockedChangedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static User toEntity(UserModel model) {
        if (model == null) {
            return null;
        }
        return User.builder()
                .id(model.getId())
                .name(model.getName())
                .email(model.getEmail())
                .passwordHash(model.getPasswordHash())
                .role(model.getRole())
                .blocked(model.isBlocked())
                .profilePrivate(model.isProfilePrivate())
                .roleChangedAt(model.getRoleChangedAt())
                .roleChangedBy(model.getRoleChangedBy())
                .blockedChangedAt(model.getBlockedChangedAt())
                .blockedChangedBy(model.getBlockedChangedBy())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    public static UserDto toDto(UserModel model) {
        if (model == null) {
            return null;
        }
        return UserDto.builder()
                .id(model.getId())
                .name(model.getName())
                .email(model.getEmail())
                .role(model.getRole())
                .blocked(model.isBlocked())
                .profilePrivate(model.isProfilePrivate())
                .roleChangedAt(model.getRoleChangedAt())
                .roleChangedBy(model.getRoleChangedBy())
                .blockedChangedAt(model.getBlockedChangedAt())
                .blockedChangedBy(model.getBlockedChangedBy())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
