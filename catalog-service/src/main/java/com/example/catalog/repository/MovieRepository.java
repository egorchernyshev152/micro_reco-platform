package com.example.catalog.repository;

import com.example.catalog.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    List<Movie> findByIdIn(Collection<Long> ids);

    Optional<Movie> findByTmdbId(Long tmdbId);

    @Query("select distinct g from Movie m join m.genres g order by g asc")
    List<String> findDistinctGenres();

    @Query("select distinct c from Movie m join m.countries c order by c asc")
    List<String> findDistinctCountries();

    @Query("select distinct t from Movie m join m.tags t order by t asc")
    List<String> findDistinctTags();

    @Query("select min(m.releaseYear) from Movie m where m.releaseYear is not null")
    Integer findMinReleaseYear();

    @Query("select max(m.releaseYear) from Movie m where m.releaseYear is not null")
    Integer findMaxReleaseYear();
}
