package com.example.catalog.service;

import com.example.catalog.dto.UserFollowListItemDto;
import com.example.catalog.dto.UserFollowStatusDto;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserFollow;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.UserFollowRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFollowService {

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;

    @Transactional
    public UserFollowStatusDto follow(Long followerId, Long targetId) {
        validateIds(followerId, targetId);
        User follower = getUser(followerId);
        User target = getUser(targetId);
        if (followRepository.existsByFollower_IdAndTarget_Id(followerId, targetId)) {
            return buildStatus(followerId, targetId);
        }
        UserFollow follow = UserFollow.builder()
                .follower(follower)
                .target(target)
                .build();
        followRepository.save(follow);
        return buildStatus(followerId, targetId);
    }

    @Transactional
    public UserFollowStatusDto unfollow(Long followerId, Long targetId) {
        validateIds(followerId, targetId);
        getUser(targetId);
        UserFollow follow = followRepository.findByFollower_IdAndTarget_Id(followerId, targetId)
                .orElse(null);
        if (follow != null) {
            followRepository.delete(follow);
        }
        return buildStatus(followerId, targetId);
    }

    @Transactional(readOnly = true)
    public UserFollowStatusDto getStatus(Long followerId, Long targetId) {
        if (followerId == null || targetId == null) {
            throw new IllegalArgumentException("Не указан пользователь");
        }
        getUser(targetId);
        return buildStatus(followerId, targetId);
    }

    @Transactional(readOnly = true)
    public List<UserFollowListItemDto> getFollowing(Long userId, int limit) {
        getUser(userId);
        return followRepository.findByFollower_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, enforceLimit(limit)))
                .stream()
                .map(follow -> mapItem(follow.getTarget(), userId, follow.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserFollowListItemDto> getFollowers(Long userId, int limit) {
        getUser(userId);
        return followRepository.findByTarget_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, enforceLimit(limit)))
                .stream()
                .map(follow -> mapItem(follow.getFollower(), userId, follow.getCreatedAt()))
                .toList();
    }

    private int enforceLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    private void validateIds(Long followerId, Long targetId) {
        if (followerId == null) {
            throw new IllegalArgumentException("Не указан пользователь");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("Не указан профиль");
        }
        if (followerId.equals(targetId)) {
            throw new IllegalArgumentException("Нельзя подписаться на самого себя");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private UserFollowStatusDto buildStatus(Long followerId, Long targetId) {
        long followersCount = followRepository.countByTarget_Id(targetId);
        long followingCount = followRepository.countByFollower_Id(targetId);
        boolean following = followRepository.existsByFollower_IdAndTarget_Id(followerId, targetId);
        return UserFollowStatusDto.builder()
                .followersCount(followersCount)
                .followingCount(followingCount)
                .following(following)
                .build();
    }

    private UserFollowListItemDto mapItem(User profile, Long ownerId, java.time.Instant followedAt) {
        boolean mutual = followRepository.existsByFollower_IdAndTarget_Id(profile.getId(), ownerId);
        return UserFollowListItemDto.builder()
                .id(profile.getId())
                .name(profile.getName())
                .profilePrivate(profile.isProfilePrivate())
                .mutual(mutual)
                .followedAt(followedAt)
                .build();
    }
}
