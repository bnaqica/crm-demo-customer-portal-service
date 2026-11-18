package com.bnaqica.crmdemocustomerportalservice.repository;

import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndOwnerSub(UUID id, UUID ownerSub);

    List<Document> findAllByOwnerSubOrderByCreatedAtDesc(UUID ownerSub);

    Optional<Document> findByStorageKey(String storageKey);

    List<Document> findAllByIdInAndOwnerSub(Collection<UUID> ids, UUID ownerSub);

    /** Bytes of the owner's CLEAN documents, and of PENDING ones requested since {@code pendingSince}. */
    @Query("SELECT COALESCE(SUM(d.sizeBytes), 0) FROM Document d WHERE d.ownerSub = :owner AND (d.status = 'CLEAN'"
            + " OR (d.status = 'PENDING' AND d.createdAt >= :pendingSince))")
    long quotaUsed(@Param("owner") UUID owner, @Param("pendingSince") Instant pendingSince);
}
