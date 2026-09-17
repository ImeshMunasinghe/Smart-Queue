package com.smartqueue.repository;

import com.smartqueue.model.ServiceTimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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

    /**
     * Analytics: Average service duration (seconds) grouped by hour-of-day (0–23).
     * Used to plot the hourly wait-time chart in the Admin Console.
     * Returns Object[] rows: [hourOfDay (Double), avgDurationSeconds (Double)]
     */
    @Query("""
        SELECT EXTRACT(HOUR FROM s.startedAt), AVG(s.durationSeconds)
        FROM ServiceTimeLog s
        WHERE s.officeId = :officeId
          AND s.startedAt >= :from
        GROUP BY EXTRACT(HOUR FROM s.startedAt)
        ORDER BY EXTRACT(HOUR FROM s.startedAt) ASC
        """)
    List<Object[]> findAvgDurationByHour(
            @Param("officeId") UUID officeId,
            @Param("from") OffsetDateTime from);
}

