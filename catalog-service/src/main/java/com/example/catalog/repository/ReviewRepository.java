package com.example.catalog.repository;

import com.example.catalog.entity.Review;
import com.example.catalog.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    Optional<Review> findByMovie_IdAndAuthor_Id(Long movieId, Long authorId);

    Page<Review> findByMovie_IdAndStatus(Long movieId, ReviewStatus status, Pageable pageable);
}
