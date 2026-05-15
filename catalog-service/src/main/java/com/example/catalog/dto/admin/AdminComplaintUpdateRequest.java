package com.example.catalog.dto.admin;

import com.example.catalog.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminComplaintUpdateRequest {

    @NotNull
    private ComplaintStatus status;
}
