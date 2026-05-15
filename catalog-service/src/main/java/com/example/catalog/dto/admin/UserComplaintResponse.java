package com.example.catalog.dto.admin;

import com.example.catalog.entity.ComplaintStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserComplaintResponse {
    Long id;
    String category;
    String description;
    ComplaintStatus status;
    String reporterName;
    String reporterEmail;
    Instant createdAt;
    Instant updatedAt;
}

