package com.example.catalog.dto;

import com.example.catalog.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpsertRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private UserRole role;

    @Size(min = 8, max = 128, message = "Пароль должен быть от 8 до 128 символов")
    private String password;

    private Boolean profilePrivate;
}
