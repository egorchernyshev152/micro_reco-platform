package com.example.catalog.controller.admin;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.admin.AdminComplaintUpdateRequest;
import com.example.catalog.dto.admin.AdminUserBlockRequest;
import com.example.catalog.dto.admin.AdminUserComplaintsResponse;
import com.example.catalog.dto.admin.AdminUserResponse;
import com.example.catalog.dto.admin.AdminUserRoleRequest;
import com.example.catalog.dto.admin.UserAuditLogResponse;
import com.example.catalog.dto.admin.UserComplaintResponse;
import com.example.catalog.security.UserPrincipal;
import com.example.catalog.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Users", description = "Административные операции со списком пользователей")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Поиск пользователей для админки",
            description = "Постраничный поиск с фильтрами по имени/email, роли и статусу блокировки")
    public PageResponse<AdminUserResponse> listUsers(
            @Parameter(description = "Поисковый запрос по имени или email") @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Фильтр роли (USER/ADMIN)") @RequestParam(value = "role", required = false) String role,
            @Parameter(description = "Фильтр блокировки (true/false)") @RequestParam(value = "blocked", required = false) Boolean blocked,
            @Parameter(description = "Номер страницы, начиная с 0") @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Размер страницы") @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Сортировка, например createdAt,desc") @RequestParam(value = "sort", required = false) String sort
    ) {
        return adminUserService.search(query, role, blocked, page, size, sort);
    }

    @PatchMapping(path = "/{userId}/block", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Изменить статус блокировки пользователя")
    public AdminUserResponse updateBlockStatus(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody AdminUserBlockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return adminUserService.updateBlockStatus(userId, request.getBlocked(), request.getReason(), principal);
    }

    @PatchMapping(path = "/{userId}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Сменить роль пользователя")
    public AdminUserResponse updateRole(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody AdminUserRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return adminUserService.updateRole(userId, request.getRole(), request.getReason(), principal);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя")
    public void deleteUser(@PathVariable("userId") Long userId,
                           @AuthenticationPrincipal UserPrincipal principal) {
        adminUserService.deleteUser(userId, principal);
    }

    @GetMapping("/{userId}/complaints")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить жалобы на пользователя")
    public AdminUserComplaintsResponse getComplaints(@PathVariable("userId") Long userId) {
        return adminUserService.getComplaints(userId);
    }

    @PatchMapping(path = "/{userId}/complaints/{complaintId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Изменить статус жалобы на пользователя")
    public UserComplaintResponse updateComplaintStatus(
            @PathVariable("userId") Long userId,
            @PathVariable("complaintId") Long complaintId,
            @Valid @RequestBody AdminComplaintUpdateRequest request
    ) {
        return adminUserService.updateComplaintStatus(userId, complaintId, request.getStatus());
    }

    @GetMapping("/{userId}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Аудит-лог действий с пользователем")
    public List<UserAuditLogResponse> getAuditLog(@PathVariable("userId") Long userId) {
        return adminUserService.getAuditLog(userId);
    }
}
