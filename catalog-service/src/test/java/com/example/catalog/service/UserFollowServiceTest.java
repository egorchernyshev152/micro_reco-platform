package com.example.catalog.service;

import com.example.catalog.dto.UserFollowListItemDto;
import com.example.catalog.dto.UserFollowStatusDto;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserFollow;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.UserFollowRepository;
import com.example.catalog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFollowRepository followRepository;

    @InjectMocks
    private UserFollowService followService;

    private User follower;
    private User target;

    @BeforeEach
    void init() {
        follower = User.builder().id(1L).name("Alice").build();
        target = User.builder().id(2L).name("Bob").build();
    }

    @Test
    void followCreatesLink() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(followRepository.existsByFollower_IdAndTarget_Id(1L, 2L)).thenReturn(false, true);
        when(followRepository.countByTarget_Id(2L)).thenReturn(1L);
        when(followRepository.countByFollower_Id(2L)).thenReturn(0L);

        UserFollowStatusDto dto = followService.follow(1L, 2L);

        verify(followRepository).save(any(UserFollow.class));
        assertThat(dto.isFollowing()).isTrue();
        assertThat(dto.getFollowersCount()).isEqualTo(1L);
    }

    @Test
    void followFailsWhenTargetMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> followService.follow(1L, 9L));
    }

    @Test
    void followingListReturnsItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
        UserFollow link = UserFollow.builder()
                .follower(follower)
                .target(target)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        when(followRepository.findByFollower_IdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(List.of(link));
        when(followRepository.existsByFollower_IdAndTarget_Id(2L, 1L)).thenReturn(false);

        List<UserFollowListItemDto> list = followService.getFollowing(1L, 10);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(2L);
    }
}
