package com.qlsk.repository;

import com.qlsk.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderBySeatRowAscSeatColAsc(Long eventId);
    Optional<Seat> findByEventIdAndSeatCode(Long eventId, String seatCode);
    long countByEventId(Long eventId);
    long countByEventIdAndIsBookedTrue(Long eventId);
    long countByEventIdAndIsBookedFalse(Long eventId);
}
