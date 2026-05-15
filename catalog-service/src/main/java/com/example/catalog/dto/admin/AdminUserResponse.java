package com.example.catalog.dto.admin;

import com.example.catalog.entity.UserRole;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminUserResponse {
    Long id;
    String name;
    String email;
    UserRole role;
    boolean blocked;
    long complaintsCount;
    Instant createdAt;
    Instant updatedAt;
    Instant roleChangedAt;
    String roleChangedBy;
    Instant blockedChangedAt;
    String blockedChangedBy;
}
