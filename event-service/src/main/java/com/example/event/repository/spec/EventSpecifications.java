package com.example.event.repository.spec;

import com.example.event.entity.Event;
import com.example.event.model.EventType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;

public final class EventSpecifications {
    private EventSpecifications() {
    }

    public static Specification<Event> userId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Event> movieId(Long movieId) {
        return (root, query, cb) -> movieId == null ? null : cb.equal(root.get("movieId"), movieId);
    }

    public static Specification<Event> type(EventType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Event> createdAfter(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Event> source(String source) {
        return (root, query, cb) -> !StringUtils.hasText(source) ? null : cb.equal(root.get("source"), source);
    }

    public static Specification<Event> sessionId(String sessionId) {
        return (root, query, cb) -> !StringUtils.hasText(sessionId) ? null : cb.equal(root.get("sessionId"), sessionId);
    }
}
