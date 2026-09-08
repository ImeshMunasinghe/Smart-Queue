package com.smartqueue.repository;

import com.smartqueue.model.SlotCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotCapacityRepository extends JpaRepository<SlotCapacity, UUID> {

    Optional<SlotCapacity> findByOfficeIdAndServiceTypeIdAndSessionDate(
            UUID officeId, UUID serviceTypeId, LocalDate sessionDate);

    List<SlotCapacity> findByOfficeIdAndSessionDate(UUID officeId, LocalDate sessionDate);

    /**
     * FR-1.2 & NFR-6.2: Atomic conditional update preventing overselling under high concurrency.
     * Atomically increments issuedCount and activeWaitingCount ONLY IF issuedCount < effective limit.
     * Returns 1 if slot successfully reserved, 0 if capacity reached.
     */
    @Modifying
    @Query("""
        UPDATE SlotCapacity s
        SET s.issuedCount = s.issuedCount + 1,
            s.activeWaitingCount = s.activeWaitingCount + 1,
            s.version = s.version + 1
        WHERE s.officeId = :officeId
          AND s.id = :slotId
          AND s.issuedCount < COALESCE(s.manualOverrideLimit, CASE WHEN s.computedLimit > 0 THEN s.computedLimit ELSE s.rawCapacity END)
        """)
    int reserveSlotAtomically(@Param("officeId") UUID officeId, @Param("slotId") UUID slotId);

    /**
     * Decrements activeWaitingCount when a token is cancelled or completed.
     */
    @Modifying
    @Query("""
        UPDATE SlotCapacity s
        SET s.activeWaitingCount = CASE WHEN s.activeWaitingCount > 0 THEN s.activeWaitingCount - 1 ELSE 0 END,
            s.version = s.version + 1
        WHERE s.id = :slotId
        """)
    int decrementActiveWaitingCount(@Param("slotId") UUID slotId);
}
