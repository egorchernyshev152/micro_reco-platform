package com.example.catalog.service;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.UserProfileDto;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserMovieCollectionType;
import com.example.catalog.exception.ForbiddenException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.UserFollowRepository;
import com.example.catalog.repository.UserMovieCollectionRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMovieCollectionRepository collectionRepository;
    private final UserMovieCollectionService collectionService;
    private final UserFollowRepository followRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = getUser(userId);
        return buildProfile(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getPublicProfile(Long userId) {
        User user = getVisibleUser(userId);
        return buildProfile(user);
    }

    @Transactional
    public UserProfileDto updatePrivacy(Long userId, boolean profilePrivate) {
        User user = getUser(userId);
        user.setProfilePrivate(profilePrivate);
        return buildProfile(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<MovieDto> getPublicCollection(Long userId, UserMovieCollectionType type) {
        getVisibleUser(userId);
        return collectionService.getCollection(userId, type);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private User getVisibleUser(Long userId) {
        User user = getUser(userId);
        if (user.isProfilePrivate()) {
            throw new ForbiddenException("Профиль пользователя скрыт настройками приватности");
        }
        return user;
    }

    private UserProfileDto buildProfile(User user) {
        Map<UserMovieCollectionType, Long> counts = new EnumMap<>(UserMovieCollectionType.class);
        for (UserMovieCollectionType type : UserMovieCollectionType.values()) {
            counts.put(type, collectionRepository.countByUserIdAndType(user.getId(), type));
        }
        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .profilePrivate(user.isProfilePrivate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .favoritesCount(counts.getOrDefault(UserMovieCollectionType.FAVORITE, 0L))
                .watchlistCount(counts.getOrDefault(UserMovieCollectionType.WATCHLIST, 0L))
                .watchedCount(counts.getOrDefault(UserMovieCollectionType.WATCHED, 0L))
                .followersCount(followRepository.countByTarget_Id(user.getId()))
                .followingCount(followRepository.countByFollower_Id(user.getId()))
                .build();
    }
}
