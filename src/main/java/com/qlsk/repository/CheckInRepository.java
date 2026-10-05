package com.qlsk.repository;

import com.qlsk.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    Optional<CheckIn> findByRegistrationId(Long registrationId);
    boolean existsByRegistrationId(Long registrationId);
    List<CheckIn> findByEventIdOrderByCheckInTimeDesc(Long eventId);
    long countByEventId(Long eventId);
}
