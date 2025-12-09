package com.example.catalog.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserModel {
    Long id;
    String name;
    String email;
}
