package com.example.catalog.dto.admin;

import com.example.catalog.entity.ReviewStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AdminReviewBulkActionRequest {

    @NotEmpty
    private List<Long> ids;

    @NotNull
    private ReviewStatus status;

    @Size(max = 512)
    private String reason;
}
