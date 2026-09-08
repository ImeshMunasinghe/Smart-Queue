package com.smartqueue.repository;

import com.smartqueue.model.ServiceTimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceTimeLogRepository extends JpaRepository<ServiceTimeLog, UUID> {
    List<ServiceTimeLog> findByOfficeIdAndServiceTypeId(UUID officeId, UUID serviceTypeId);

    @Query("""
        SELECT AVG(s.durationSeconds) FROM ServiceTimeLog s
        WHERE s.officeId = :officeId
          AND s.serviceTypeId = :serviceTypeId
        """)
    Double findAverageDurationSeconds(
            @Param("officeId") UUID officeId,
            @Param("serviceTypeId") UUID serviceTypeId);
}
