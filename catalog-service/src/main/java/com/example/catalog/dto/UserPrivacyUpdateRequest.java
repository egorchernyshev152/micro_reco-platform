package com.example.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserPrivacyUpdateRequest {

    @NotNull
    private Boolean profilePrivate;
}
