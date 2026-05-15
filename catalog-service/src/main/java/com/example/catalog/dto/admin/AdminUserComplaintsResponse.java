package com.example.catalog.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminUserComplaintsResponse {
    long openCount;
    long reviewingCount;
    long resolvedCount;
    List<UserComplaintResponse> complaints;
}

