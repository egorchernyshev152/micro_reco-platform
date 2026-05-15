package com.example.catalog.controller;

import com.example.catalog.dto.UserFollowListItemDto;
import com.example.catalog.dto.UserFollowStatusDto;
import com.example.catalog.security.UserPrincipal;
import com.example.catalog.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User follow", description = "Подписки на пользователей")
public class UserFollowController {

    private final UserFollowService followService;

    @PostMapping("/{targetUserId}/follow")
    @Operation(summary = "Подписаться на пользователя")
    @PreAuthorize("isAuthenticated()")
    public UserFollowStatusDto follow(@PathVariable("targetUserId") @Min(1) Long targetUserId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return followService.follow(principal.getId(), targetUserId);
    }

    @DeleteMapping("/{targetUserId}/follow")
    @Operation(summary = "Перестать следить за пользователем")
    @PreAuthorize("isAuthenticated()")
    public UserFollowStatusDto unfollow(@PathVariable("targetUserId") @Min(1) Long targetUserId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return followService.unfollow(principal.getId(), targetUserId);
    }

    @GetMapping("/{targetUserId}/follow/status")
    @Operation(summary = "Статус подписки на пользователя")
    @PreAuthorize("isAuthenticated()")
    public UserFollowStatusDto status(@PathVariable("targetUserId") @Min(1) Long targetUserId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return followService.getStatus(principal.getId(), targetUserId);
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "Список пользователей, на которых подписан владелец")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public List<UserFollowListItemDto> following(@PathVariable("userId") @Min(1) Long userId,
                                                 @RequestParam(value = "limit", defaultValue = "12") @Min(1) int limit) {
        return followService.getFollowing(userId, limit);
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Список подписчиков пользователя")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public List<UserFollowListItemDto> followers(@PathVariable("userId") @Min(1) Long userId,
                                                 @RequestParam(value = "limit", defaultValue = "12") @Min(1) int limit) {
        return followService.getFollowers(userId, limit);
    }
}
