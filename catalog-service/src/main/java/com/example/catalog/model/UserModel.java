package com.example.catalog.model;

import com.example.catalog.entity.UserRole;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserModel {
    Long id;
    String name;
    String email;
    String passwordHash;
    UserRole role;
    boolean blocked;
    boolean profilePrivate;
    Instant roleChangedAt;
    String roleChangedBy;
    Instant blockedChangedAt;
    String blockedChangedBy;
    Instant createdAt;
    Instant updatedAt;
}
