package com.splitwise.repo;

import com.splitwise.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
