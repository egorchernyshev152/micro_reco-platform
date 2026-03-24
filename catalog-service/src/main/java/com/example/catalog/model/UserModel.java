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
    Instant createdAt;
    Instant updatedAt;
}
