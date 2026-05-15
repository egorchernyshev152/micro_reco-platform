package com.example.catalog.repository.spec;

import com.example.catalog.entity.Movie;
import com.example.catalog.entity.MovieCastMember;
import com.example.catalog.entity.MovieStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;
import java.util.Set;

public final class MovieSpecifications {
    private MovieSpecifications() {
    }

    public static Specification<Movie> queryText(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return null;
            }
            String like = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("originalTitle")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("synopsis")), like)
            );
        };
    }

    public static Specification<Movie> withGenres(Set<String> genres) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(genres)) {
                return null;
            }
            query.distinct(true);
            SetJoin<Movie, String> join = root.joinSet("genres", JoinType.INNER);
            return join.in(genres);
        };
    }

    public static Specification<Movie> withCountries(Set<String> countries) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(countries)) {
                return null;
            }
            query.distinct(true);
            SetJoin<Movie, String> join = root.joinSet("countries", JoinType.INNER);
            return join.in(countries);
        };
    }

    public static Specification<Movie> withTags(Set<String> tags) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(tags)) {
                return null;
            }
            query.distinct(true);
            SetJoin<Movie, String> join = root.joinSet("tags", JoinType.INNER);
            return join.in(tags);
        };
    }

    public static Specification<Movie> withCast(Set<String> names) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(names)) {
                return null;
            }
            query.distinct(true);
            ListJoin<Movie, MovieCastMember> join = root.joinList("cast", JoinType.INNER);
            Predicate[] predicates = names.stream()
                    .filter(StringUtils::hasText)
                    .map(name -> cb.like(cb.lower(join.get("name")), "%" + name.trim().toLowerCase() + "%"))
                    .toArray(Predicate[]::new);
            if (predicates.length == 0) {
                return null;
            }
            return cb.or(predicates);
        };
    }

    public static Specification<Movie> releaseYearFrom(Integer year) {
        return (root, query, cb) -> year == null ? null : cb.greaterThanOrEqualTo(root.get("releaseYear"), year);
    }

    public static Specification<Movie> releaseYearTo(Integer year) {
        return (root, query, cb) -> year == null ? null : cb.lessThanOrEqualTo(root.get("releaseYear"), year);
    }

    public static Specification<Movie> durationFrom(Integer minutes) {
        return (root, query, cb) -> minutes == null ? null : cb.greaterThanOrEqualTo(root.get("durationMinutes"), minutes);
    }

    public static Specification<Movie> durationTo(Integer minutes) {
        return (root, query, cb) -> minutes == null ? null : cb.lessThanOrEqualTo(root.get("durationMinutes"), minutes);
    }

    public static Specification<Movie> ratingFrom(Double rating) {
        return (root, query, cb) -> rating == null ? null : cb.greaterThanOrEqualTo(root.get("averageRating"), rating);
    }

    public static Specification<Movie> ratingTo(Double rating) {
        return (root, query, cb) -> rating == null ? null : cb.lessThanOrEqualTo(root.get("averageRating"), rating);
    }

    public static Specification<Movie> withStatuses(Set<MovieStatus> statuses) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(statuses)) {
                return null;
            }
            return root.get("status").in(statuses);
        };
    }
}
