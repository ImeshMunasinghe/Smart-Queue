package com.smartqueue.repository;

import com.smartqueue.model.Token;
import com.smartqueue.model.TokenState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByOfficeIdAndIdempotencyKey(UUID officeId, String idempotencyKey);

    /**
     * FR-1.3: Prevents a single citizen from holding more than one active token per service type.
     */
    @Query("""
        SELECT COUNT(t) FROM Token t
        WHERE t.officeId = :officeId
          AND t.citizenReferenceHash = :citizenHash
          AND t.serviceTypeId = :serviceTypeId
          AND t.state IN ('WAITING', 'CALLED', 'SERVING', 'SKIPPED')
        """)
    long countActiveTokensForCitizen(
            @Param("officeId") UUID officeId,
            @Param("citizenHash") String citizenHash,
            @Param("serviceTypeId") UUID serviceTypeId);

    /**
     * Fetches the active waiting queue for a given service type, ordered by priority desc and creation time asc.
     */
    @Query("""
        SELECT t FROM Token t
        WHERE t.officeId = :officeId
          AND t.serviceTypeId = :serviceTypeId
          AND t.state = 'WAITING'
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    List<Token> findWaitingQueueByServiceType(
            @Param("officeId") UUID officeId,
            @Param("serviceTypeId") UUID serviceTypeId);

    /**
     * Fetches next waiting token assigned to a specific counter or general pool.
     */
    @Query("""
        SELECT t FROM Token t
        WHERE t.officeId = :officeId
          AND (t.assignedCounterId = :counterId OR (t.assignedCounterId IS NULL AND t.serviceTypeId IN :eligibleServiceTypes))
          AND t.state = 'WAITING'
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    List<Token> findNextTokensForCounter(
            @Param("officeId") UUID officeId,
            @Param("counterId") UUID counterId,
            @Param("eligibleServiceTypes") List<UUID> eligibleServiceTypes);

    /**
     * Finds tokens in CALLED state that have exceeded the response timeout.
     */
    @Query("""
        SELECT t FROM Token t
        WHERE t.state = 'CALLED'
          AND t.calledAt < :timeoutThreshold
        """)
    List<Token> findExpiredCalledTokens(@Param("timeoutThreshold") OffsetDateTime timeoutThreshold);

    /**
     * Counts how many tokens are ahead of the given token in WAITING state.
     */
    @Query("""
        SELECT COUNT(t) FROM Token t
        WHERE t.officeId = :officeId
          AND t.serviceTypeId = :serviceTypeId
          AND t.state = 'WAITING'
          AND (t.priority > :priority OR (t.priority = :priority AND t.createdAt < :createdAt))
        """)
    long countTokensAhead(
            @Param("officeId") UUID officeId,
            @Param("serviceTypeId") UUID serviceTypeId,
            @Param("priority") int priority,
            @Param("createdAt") OffsetDateTime createdAt);

    List<Token> findByAssignedCounterIdAndState(UUID counterId, TokenState state);
    
    List<Token> findByOfficeIdAndState(UUID officeId, TokenState state);
}
