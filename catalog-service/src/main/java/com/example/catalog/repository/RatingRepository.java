package com.example.catalog.repository;

import com.example.catalog.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUser_Id(Long userId);
    List<Rating> findByMovie_Id(Long movieId);
    boolean existsByUser_IdAndMovie_Id(Long userId, Long movieId);
    boolean existsByUser_IdAndMovie_IdAndIdNot(Long userId, Long movieId, Long id);
    long countByMovie_Id(Long movieId);
    Optional<Rating> findByUser_IdAndMovie_Id(Long userId, Long movieId);

    @org.springframework.data.jpa.repository.Query("select avg(r.score) from Rating r where r.movie.id = :movieId")
    Double calculateAverageScore(Long movieId);
}
