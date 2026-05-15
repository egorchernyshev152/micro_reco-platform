package com.example.catalog.service.admin;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.admin.AdminUserComplaintsResponse;
import com.example.catalog.dto.admin.AdminUserResponse;
import com.example.catalog.entity.ComplaintStatus;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserComplaint;
import com.example.catalog.entity.UserRole;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.repository.UserAuditLogRepository;
import com.example.catalog.repository.UserComplaintRepository;
import com.example.catalog.repository.UserRepository;
import com.example.catalog.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserComplaintRepository userComplaintRepository;

    @Mock
    private UserAuditLogRepository userAuditLogRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void searchShouldIncludeComplaintCounts() {
        User user = user(10L, "Test", "test@mail.com", UserRole.USER, false);
        when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));
        when(userComplaintRepository.countActiveComplaintsByTarget(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        PageResponse<AdminUserResponse> response = adminUserService.search(null, null, null, 0, 20, null);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getComplaintsCount()).isEqualTo(3);
    }

    @Test
    void updateBlockStatusShouldToggleAndLog() {
        User user = user(42L, "Demo", "demo@mail.com", UserRole.USER, false);
        UserPrincipal actor = adminPrincipal();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userComplaintRepository.countActiveComplaintsByTarget(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{42L, 2L}));

        AdminUserResponse updated = adminUserService.updateBlockStatus(42L, true, "Спам", actor);

        assertThat(updated.isBlocked()).isTrue();
        assertThat(updated.getBlockedChangedBy()).isEqualTo(actor.getUsername());
        assertThat(updated.getComplaintsCount()).isEqualTo(2);
        verify(userAuditLogRepository).save(argThat(log -> log.getAction().name().equals("BLOCK_UPDATED")));
    }

    @Test
    void updateRoleShouldThrowWhenTryingToDemoteLastAdmin() {
        User admin = user(5L, "Admin", "admin@example.com", UserRole.ADMIN, false);
        UserPrincipal actor = adminPrincipal();
        when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateRole(5L, UserRole.USER, "reason", actor))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(userAuditLogRepository);
    }

    @Test
    void getComplaintsShouldAggregateStatuses() {
        User target = user(7L, "Target", "target@mail.com", UserRole.USER, false);
        User reporter = user(8L, "Reporter", "reporter@mail.com", UserRole.USER, false);
        Instant now = Instant.now();
        UserComplaint pending = UserComplaint.builder()
                .id(1L)
                .targetUser(target)
                .reporter(reporter)
                .category("Abuse")
                .description("text")
                .status(ComplaintStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        UserComplaint reviewing = UserComplaint.builder()
                .id(2L)
                .targetUser(target)
                .reporter(reporter)
                .category("Spam")
                .description("S")
                .status(ComplaintStatus.REVIEWING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        UserComplaint resolved = UserComplaint.builder()
                .id(3L)
                .targetUser(target)
                .reporter(reporter)
                .category("Fraud")
                .description("R")
                .status(ComplaintStatus.RESOLVED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(userRepository.findById(7L)).thenReturn(Optional.of(target));
        when(userComplaintRepository.findByTargetUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(pending, reviewing, resolved));

        AdminUserComplaintsResponse response = adminUserService.getComplaints(7L);

        assertThat(response.getComplaints()).hasSize(3);
        assertThat(response.getOpenCount()).isEqualTo(1);
        assertThat(response.getReviewingCount()).isEqualTo(1);
        assertThat(response.getResolvedCount()).isEqualTo(1);
    }

    @Test
    void deleteUserShouldPreventSelfRemoval() {
        User user = user(55L, "Self", "self@mail.com", UserRole.USER, false);
        when(userRepository.findById(55L)).thenReturn(Optional.of(user));
        UserPrincipal principal = UserPrincipal.from(user);

        assertThatThrownBy(() -> adminUserService.deleteUser(55L, principal))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).delete(any(User.class));
    }

    private User user(Long id, String name, String email, UserRole role, boolean blocked) {
        Instant now = Instant.now();
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .passwordHash("hash")
                .role(role)
                .blocked(blocked)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserPrincipal adminPrincipal() {
        User admin = user(100L, "Root", "root@mail.com", UserRole.ADMIN, false);
        return UserPrincipal.from(admin);
    }
}
