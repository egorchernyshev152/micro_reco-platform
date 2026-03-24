package com.example.event.repository;

import com.example.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("select e.movieId as movieId, count(e.id) as cnt from Event e " +
            "where e.createdAt >= coalesce(:from, e.createdAt) " +
            "group by e.movieId order by cnt desc")
    List<Object[]> countByMovie(@Param("from") Instant from);

    @Query("select e.userId as userId, count(e.id) as cnt from Event e " +
            "where e.createdAt >= coalesce(:from, e.createdAt) " +
            "group by e.userId order by cnt desc")
    List<Object[]> countByUser(@Param("from") Instant from);

    @Query("select function('to_char', e.createdAt, 'YYYY-MM-DD') as day, count(e.id) as cnt from Event e " +
            "where e.createdAt >= coalesce(:from, e.createdAt) " +
            "group by function('to_char', e.createdAt, 'YYYY-MM-DD') order by day asc")
    List<Object[]> countByDay(@Param("from") Instant from);

    @Query("select function('to_char', e.createdAt, 'YYYY-MM-DD HH24:00') as bucket, count(e.id) as cnt from Event e " +
            "where e.createdAt >= coalesce(:from, e.createdAt) " +
            "group by function('to_char', e.createdAt, 'YYYY-MM-DD HH24:00') order by bucket asc")
    List<Object[]> countByHour(@Param("from") Instant from);
}
