package com.example.catalog.repository.spec;

import com.example.catalog.entity.User;
import com.example.catalog.entity.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> withFilters(String query, UserRole role, Boolean blocked) {
        Specification<User> spec = Specification.where(null);
        if (StringUtils.hasText(query)) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), like),
                            cb.like(cb.lower(root.get("email")), like)
                    )
            );
        }
        if (role != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("role"), role));
        }
        if (blocked != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("blocked"), blocked));
        }
        return spec;
    }
}
