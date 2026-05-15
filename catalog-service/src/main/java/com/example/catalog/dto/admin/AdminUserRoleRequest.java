package com.example.catalog.dto.admin;

import com.example.catalog.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserRoleRequest {
    @NotNull
    private UserRole role;
    private String reason;
}

