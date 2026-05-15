package com.example.catalog.dto.auth;

import com.example.catalog.entity.UserRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthenticatedUserDto {
    Long id;
    String name;
    String email;
    UserRole role;
}
