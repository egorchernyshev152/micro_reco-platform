package com.example.catalog.controller;

import com.example.catalog.dto.UserPrivacyUpdateRequest;
import com.example.catalog.dto.UserProfileDto;
import com.example.catalog.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Настройки и данные профиля пользователя")
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping
    @Operation(summary = "Получить профиль пользователя (для владельца)")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public UserProfileDto getProfile(@PathVariable("userId") Long userId) {
        return profileService.getProfile(userId);
    }

    @PutMapping("/privacy")
    @Operation(summary = "Обновить настройку приватности профиля")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public UserProfileDto updatePrivacy(@PathVariable("userId") Long userId,
                                        @Valid @RequestBody UserPrivacyUpdateRequest request) {
        return profileService.updatePrivacy(userId, request.getProfilePrivate());
    }
}
