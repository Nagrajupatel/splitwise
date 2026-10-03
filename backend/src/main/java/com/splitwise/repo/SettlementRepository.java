package com.splitwise.repo;

import com.splitwise.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByTripId(Long tripId);
}
