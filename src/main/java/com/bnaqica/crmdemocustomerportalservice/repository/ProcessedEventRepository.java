package com.bnaqica.crmdemocustomerportalservice.repository;

import com.bnaqica.crmdemocustomerportalservice.entity.ProcessedEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /** @return 1 if recorded now, 0 if the event was already processed */
    @Modifying
    @Query(
            value = "INSERT INTO {h-schema}processed_event (event_id, processed_at) VALUES (:id, :now)"
                    + " ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID eventId, @Param("now") Instant now);
}
