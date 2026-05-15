package com.example.catalog.controller;

import com.example.catalog.dto.UserComplaintCreateRequest;
import com.example.catalog.dto.admin.UserComplaintResponse;
import com.example.catalog.security.UserPrincipal;
import com.example.catalog.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/complaints", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Отправка жалоб на пользователей")
public class UserComplaintController {

    private final AdminUserService adminUserService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Создать жалобу на пользователя")
    public UserComplaintResponse createComplaint(
            @Valid @RequestBody UserComplaintCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return adminUserService.submitComplaint(request.getTargetUserId(), request.getCategory(), request.getDescription(), principal);
    }
}
