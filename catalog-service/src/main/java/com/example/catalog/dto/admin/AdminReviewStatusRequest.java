package com.example.catalog.dto.admin;

import com.example.catalog.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReviewStatusRequest {

    @NotNull
    private ReviewStatus status;

    @Size(max = 512)
    private String reason;
}
