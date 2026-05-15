package com.example.catalog.repository.spec;

import com.example.catalog.entity.Review;
import com.example.catalog.entity.ReviewStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<Review> baseFilters(String query, Long movieId, Long userId, Boolean flagged) {
        Specification<Review> spec = Specification.where(null);
        if (StringUtils.hasText(query)) {
            spec = spec.and(searchQuery(query));
        }
        if (movieId != null) {
            spec = spec.and(hasMovie(movieId));
        }
        if (userId != null) {
            spec = spec.and(hasAuthor(userId));
        }
        if (flagged != null) {
            spec = spec.and(isFlagged(flagged));
        }
        return spec;
    }

    public static Specification<Review> withStatus(Specification<Review> base, ReviewStatus status) {
        if (status == null) {
            return base;
        }
        return base.and(hasStatus(status));
    }

    public static Specification<Review> searchQuery(String query) {
        return (root, cq, cb) -> {
            String like = "%" + query.toLowerCase() + "%";
            var movieJoin = root.join("movie", JoinType.LEFT);
            var userJoin = root.join("author", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("content")), like),
                    cb.like(cb.lower(movieJoin.get("title")), like),
                    cb.like(cb.lower(userJoin.get("name")), like),
                    cb.like(cb.lower(userJoin.get("email")), like)
            );
        };
    }

    public static Specification<Review> hasMovie(Long movieId) {
        return (root, cq, cb) -> cb.equal(root.get("movie").get("id"), movieId);
    }

    public static Specification<Review> hasAuthor(Long userId) {
        return (root, cq, cb) -> cb.equal(root.get("author").get("id"), userId);
    }

    public static Specification<Review> isFlagged(boolean flagged) {
        return (root, cq, cb) -> cb.equal(root.get("flagged"), flagged);
    }

    public static Specification<Review> hasStatus(ReviewStatus status) {
        return (root, cq, cb) -> cb.equal(root.get("status"), status);
    }
}
