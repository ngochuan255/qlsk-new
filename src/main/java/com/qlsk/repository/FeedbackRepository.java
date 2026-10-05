package com.qlsk.repository;

import com.qlsk.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEventIdOrderByCreatedAtDesc(Long eventId);
    Optional<Feedback> findByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.event.id = :eventId")
    Double getAverageRatingByEventId(@Param("eventId") Long eventId);

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f WHERE f.event.id = :eventId GROUP BY f.rating")
    List<Object[]> getRatingDistributionByEventId(@Param("eventId") Long eventId);
}
