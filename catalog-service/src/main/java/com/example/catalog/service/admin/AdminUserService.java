package com.example.catalog.service.admin;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.admin.AdminUserComplaintsResponse;
import com.example.catalog.dto.admin.AdminUserResponse;
import com.example.catalog.dto.admin.UserAuditLogResponse;
import com.example.catalog.dto.admin.UserComplaintResponse;
import com.example.catalog.entity.AdminAuditAction;
import com.example.catalog.entity.ComplaintStatus;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserAuditLog;
import com.example.catalog.entity.UserComplaint;
import com.example.catalog.entity.UserRole;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.UserAuditLogRepository;
import com.example.catalog.repository.UserComplaintRepository;
import com.example.catalog.repository.UserRepository;
import com.example.catalog.repository.spec.UserSpecifications;
import com.example.catalog.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserComplaintRepository userComplaintRepository;
    private final UserAuditLogRepository userAuditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String query, String role, Boolean blocked, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        UserRole userRole = StringUtils.hasText(role) ? UserRole.valueOf(role.toUpperCase(Locale.ROOT)) : null;
        Page<User> result = userRepository.findAll(UserSpecifications.withFilters(query, userRole, blocked), pageable);
        Map<Long, Long> complaintCounts = complaintCounts(result.getContent());
        return PageResponse.<AdminUserResponse>builder()
                .items(result.getContent().stream()
                        .map(user -> mapUser(user, complaintCounts.getOrDefault(user.getId(), 0L)))
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .build();
    }

    @Transactional
    public AdminUserResponse updateBlockStatus(Long userId, boolean blocked, String reason, UserPrincipal principal) {
        User user = getUserOrThrow(userId);
        guardSelfAction(user, principal, "Нельзя менять статус собственной блокировки");
        if (user.isBlocked() == blocked) {
            return mapUser(user, activeComplaintCount(user.getId()));
        }
        user.setBlocked(blocked);
        user.setBlockedChangedAt(Instant.now());
        user.setBlockedChangedBy(actorLabel(principal));
        userRepository.save(user);
        recordAudit(user, AdminAuditAction.BLOCK_UPDATED,
                blocked ? "Заблокирован: " + defaultReason(reason) : "Разблокирован: " + defaultReason(reason),
                principal);
        return mapUser(user, activeComplaintCount(user.getId()));
    }

    @Transactional
    public AdminUserResponse updateRole(Long userId, UserRole newRole, String reason, UserPrincipal principal) {
        User user = getUserOrThrow(userId);
        if (newRole == null) {
            throw new IllegalArgumentException("Новая роль не задана");
        }
        guardSelfAction(user, principal, "Нельзя менять собственную роль");
        guardLastAdminDemotion(user, newRole);
        if (user.getRole() == newRole) {
            return mapUser(user, activeComplaintCount(user.getId()));
        }
        user.setRole(newRole);
        user.setRoleChangedAt(Instant.now());
        user.setRoleChangedBy(actorLabel(principal));
        userRepository.save(user);
        recordAudit(user, AdminAuditAction.ROLE_UPDATED,
                "Роль изменена на " + newRole + ". " + defaultReason(reason),
                principal);
        return mapUser(user, activeComplaintCount(user.getId()));
    }

    @Transactional
    public void deleteUser(Long userId, UserPrincipal principal) {
        User user = getUserOrThrow(userId);
        guardSelfAction(user, principal, "Нельзя удалить свой аккаунт");
        guardLastAdminDeletion(user);
        recordAudit(user, AdminAuditAction.USER_DELETED, "Пользователь удален администратором", principal);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public AdminUserComplaintsResponse getComplaints(Long userId) {
        getUserOrThrow(userId);
        List<UserComplaint> complaints = userComplaintRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
        Map<ComplaintStatus, Long> stats = new EnumMap<>(ComplaintStatus.class);
        for (ComplaintStatus status : ComplaintStatus.values()) {
            stats.put(status, 0L);
        }
        List<UserComplaintResponse> responses = complaints.stream()
                .map(this::mapComplaint)
                .toList();
        responses.forEach(response -> stats.computeIfPresent(response.getStatus(), (k, v) -> v + 1));
        return AdminUserComplaintsResponse.builder()
                .openCount(stats.get(ComplaintStatus.PENDING))
                .reviewingCount(stats.get(ComplaintStatus.REVIEWING))
                .resolvedCount(stats.get(ComplaintStatus.RESOLVED))
                .complaints(responses)
                .build();
    }

    @Transactional
    public UserComplaintResponse updateComplaintStatus(Long userId, Long complaintId, ComplaintStatus status) {
        getUserOrThrow(userId);
        UserComplaint complaint = userComplaintRepository.findByIdAndTargetUserId(complaintId, userId)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));
        complaint.setStatus(status);
        userComplaintRepository.save(complaint);
        return mapComplaint(complaint);
    }

    @Transactional
    public UserComplaintResponse submitComplaint(Long targetUserId, String category, String description, UserPrincipal reporter) {
        User target = getUserOrThrow(targetUserId);
        if (reporter != null && target.getId().equals(reporter.getId())) {
            throw new ConflictException("Нельзя отправить жалобу на себя");
        }
        User reporterUser = reporter != null ? getUserOrThrow(reporter.getId()) : null;
        UserComplaint complaint = UserComplaint.builder()
                .targetUser(target)
                .reporter(reporterUser)
                .category(category)
                .description(description)
                .status(ComplaintStatus.PENDING)
                .build();
        userComplaintRepository.save(complaint);
        return mapComplaint(complaint);
    }

    @Transactional(readOnly = true)
    public List<UserAuditLogResponse> getAuditLog(Long userId) {
        getUserOrThrow(userId);
        return userAuditLogRepository.findTop25ByTargetUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapAudit)
                .toList();
    }

    private Sort resolveSort(String sortParam) {
        if (!StringUtils.hasText(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] tokens = sortParam.split(",");
        String property = tokens[0].trim();
        Sort.Direction direction = tokens.length > 1 ? Sort.Direction.fromString(tokens[1].trim()) : Sort.Direction.ASC;
        if (!StringUtils.hasText(property)) {
            property = "createdAt";
        }
        return Sort.by(direction, property);
    }

    private AdminUserResponse mapUser(User user, long complaintsCount) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .blocked(user.isBlocked())
                .complaintsCount(complaintsCount)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roleChangedAt(user.getRoleChangedAt())
                .roleChangedBy(user.getRoleChangedBy())
                .blockedChangedAt(user.getBlockedChangedAt())
                .blockedChangedBy(user.getBlockedChangedBy())
                .build();
    }

    private Map<Long, Long> complaintCounts(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = users.stream().map(User::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : userComplaintRepository.countActiveComplaintsByTarget(ids)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private long activeComplaintCount(Long userId) {
        List<Object[]> rows = userComplaintRepository.countActiveComplaintsByTarget(List.of(userId));
        if (rows.isEmpty()) {
            return 0L;
        }
        return (Long) rows.get(0)[1];
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    private void guardSelfAction(User target, UserPrincipal principal, String message) {
        if (principal != null && target.getId().equals(principal.getId())) {
            throw new ConflictException(message);
        }
    }

    private void guardLastAdminDemotion(User target, UserRole newRole) {
        if (target.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            long admins = userRepository.countByRole(UserRole.ADMIN);
            if (admins <= 1) {
                throw new ConflictException("Невозможно понизить единственного администратора");
            }
        }
    }

    private void guardLastAdminDeletion(User target) {
        if (target.getRole() == UserRole.ADMIN) {
            long admins = userRepository.countByRole(UserRole.ADMIN);
            if (admins <= 1) {
                throw new ConflictException("Невозможно удалить единственного администратора");
            }
        }
    }

    private void recordAudit(User target, AdminAuditAction action, String details, UserPrincipal principal) {
        UserAuditLog log = UserAuditLog.builder()
                .targetUser(target)
                .action(action)
                .details(details)
                .performedById(principal != null ? principal.getId() : null)
                .performedByEmail(principal != null ? principal.getUsername() : "system")
                .performedByName(principal != null ? principal.getUsername() : "system")
                .build();
        userAuditLogRepository.save(log);
    }

    private String actorLabel(UserPrincipal principal) {
        return principal != null ? principal.getUsername() : "system";
    }

    private String defaultReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "Без указания причины";
        }
        return reason;
    }

    private UserComplaintResponse mapComplaint(UserComplaint complaint) {
        return UserComplaintResponse.builder()
                .id(complaint.getId())
                .category(complaint.getCategory())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .reporterName(complaint.getReporter() != null ? complaint.getReporter().getName() : "Аноним")
                .reporterEmail(complaint.getReporter() != null ? complaint.getReporter().getEmail() : null)
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }

    private UserAuditLogResponse mapAudit(UserAuditLog log) {
        return UserAuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .details(log.getDetails())
                .performedById(log.getPerformedById())
                .performedByEmail(log.getPerformedByEmail())
                .performedByName(log.getPerformedByName())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
