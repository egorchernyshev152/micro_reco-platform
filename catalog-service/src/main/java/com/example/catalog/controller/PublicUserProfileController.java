package com.example.catalog.controller;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.UserProfileDto;
import com.example.catalog.entity.UserMovieCollectionType;
import com.example.catalog.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/users")
@RequiredArgsConstructor
@Tag(name = "Public User Profiles", description = "Публичные профили и коллекции пользователей")
public class PublicUserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/{userId}")
    @Operation(summary = "Получить публичный профиль пользователя")
    public UserProfileDto getPublicProfile(@PathVariable("userId") @Min(1) Long userId) {
        return profileService.getPublicProfile(userId);
    }

    @GetMapping("/{userId}/collections/{type}")
    @Operation(summary = "Получить публичную коллекцию пользователя")
    public List<MovieDto> getPublicCollection(@PathVariable("userId") @Min(1) Long userId,
                                              @PathVariable("type") UserMovieCollectionType type) {
        return profileService.getPublicCollection(userId, type);
    }
}
