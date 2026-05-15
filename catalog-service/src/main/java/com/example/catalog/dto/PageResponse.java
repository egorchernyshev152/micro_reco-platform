package com.example.catalog.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PageResponse<T> {
    List<T> items;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean hasNext;
}
