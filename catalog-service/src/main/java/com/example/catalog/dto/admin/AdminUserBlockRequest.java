package com.example.catalog.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserBlockRequest {
    @NotNull
    private Boolean blocked;
    private String reason;
}

