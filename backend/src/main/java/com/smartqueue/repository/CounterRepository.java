package com.smartqueue.repository;

import com.smartqueue.model.Counter;
import com.smartqueue.model.CounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CounterRepository extends JpaRepository<Counter, UUID> {
    List<Counter> findByOfficeId(UUID officeId);
    List<Counter> findByOfficeIdAndStatus(UUID officeId, CounterStatus status);
    Optional<Counter> findByOfficeIdAndCounterNumber(UUID officeId, String counterNumber);
}
