package com.qlsk.repository;

import com.qlsk.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByEventIdOrderByExpenseDateDesc(Long eventId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.event.id = :eventId")
    BigDecimal sumAmountByEventId(@Param("eventId") Long eventId);
}
