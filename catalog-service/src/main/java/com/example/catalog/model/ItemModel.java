package com.example.catalog.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ItemModel {
    Long id;
    String title;
    String description;
    String category;
}
