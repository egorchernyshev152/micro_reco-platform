package com.example.catalog.dto;

import com.example.catalog.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private boolean blocked;
    private boolean profilePrivate;
    private Instant roleChangedAt;
    private String roleChangedBy;
    private Instant blockedChangedAt;
    private String blockedChangedBy;
    private Instant createdAt;
    private Instant updatedAt;
}

