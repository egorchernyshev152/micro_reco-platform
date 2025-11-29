package com.example.catalog.repository;

import com.example.catalog.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUser_Id(Long userId);
    List<Rating> findByItem_Id(Long itemId);
}

