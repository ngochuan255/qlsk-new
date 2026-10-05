package com.qlsk.repository;

import com.qlsk.entity.Registration;
import com.qlsk.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByUserIdOrderByRegisteredAtDesc(Long userId);
    List<Registration> findByEventIdOrderByRegisteredAtAsc(Long eventId);
    Optional<Registration> findByQrCodeToken(String qrCodeToken);
    Optional<Registration> findByRegistrationCode(String registrationCode);
    Optional<Registration> findByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
    long countByStatus(RegistrationStatus status);
}
