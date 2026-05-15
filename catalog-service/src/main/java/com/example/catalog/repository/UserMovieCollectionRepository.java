package com.example.catalog.repository;

import com.example.catalog.entity.UserMovieCollection;
import com.example.catalog.entity.UserMovieCollectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMovieCollectionRepository extends JpaRepository<UserMovieCollection, Long> {

    List<UserMovieCollection> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, UserMovieCollectionType type);

    Optional<UserMovieCollection> findByUserIdAndMovieIdAndType(Long userId, Long movieId, UserMovieCollectionType type);

    boolean existsByUserIdAndMovieIdAndType(Long userId, Long movieId, UserMovieCollectionType type);

    void deleteByUserIdAndMovieIdAndType(Long userId, Long movieId, UserMovieCollectionType type);

    List<UserMovieCollection> findByUserIdAndMovieIdIn(Long userId, List<Long> movieIds);

    long countByUserIdAndType(Long userId, UserMovieCollectionType type);
}
