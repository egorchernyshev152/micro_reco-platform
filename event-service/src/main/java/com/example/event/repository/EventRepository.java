package com.example.event.repository;

import com.example.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUserId(Long userId);

    @Query("select e.itemId as itemId, count(e.id) as cnt from Event e where e.createdAt >= :from group by e.itemId order by cnt desc")
    List<Object[]> countByItemSince(Instant from);

    @Query("select e.itemId as itemId, count(e.id) as cnt from Event e group by e.itemId order by cnt desc")
    List<Object[]> countByItemAll();

    @Query("select function('to_char', e.createdAt, 'YYYY-MM-DD') as day, count(e.id) as cnt from Event e where e.createdAt >= :from group by function('to_char', e.createdAt, 'YYYY-MM-DD') order by day asc")
    List<Object[]> countByDaySince(Instant from);

    @Query("select function('to_char', e.createdAt, 'YYYY-MM-DD') as day, count(e.id) as cnt from Event e group by function('to_char', e.createdAt, 'YYYY-MM-DD') order by day asc")
    List<Object[]> countByDayAll();
}

