package com.example.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @Min(1)
    @Max(10)
    private int score;

    @NotBlank
    @Size(min = 30, max = 4000)
    private String content;
}
