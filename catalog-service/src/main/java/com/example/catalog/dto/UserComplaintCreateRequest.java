package com.example.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserComplaintCreateRequest {

    @NotNull
    @Min(1)
    private Long targetUserId;

    @NotBlank
    @Size(max = 100)
    private String category;

    @NotBlank
    @Size(max = 2048)
    private String description;
}
