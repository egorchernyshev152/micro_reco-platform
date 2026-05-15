package com.example.catalog.dto.admin;

import com.example.catalog.entity.AdminAuditAction;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserAuditLogResponse {
    Long id;
    AdminAuditAction action;
    String details;
    Long performedById;
    String performedByEmail;
    String performedByName;
    Instant createdAt;
}

