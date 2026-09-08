package com.smartqueue.repository;

import com.smartqueue.model.TokenStateTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TokenStateTransitionRepository extends JpaRepository<TokenStateTransition, Long> {
    List<TokenStateTransition> findByTokenIdOrderByCreatedAtAsc(UUID tokenId);
    List<TokenStateTransition> findByTokenIdAndIdGreaterThanOrderByCreatedAtAsc(UUID tokenId, Long lastSeenId);
}
