package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD for {@code document}. */
public interface DocumentService {

    Document create(Document document);

    Optional<Document> findOwned(UUID id, UUID owner);

    List<Document> listOwned(UUID owner);

    /** The caller's documents among {@code ids}; ids of other users' documents are left out. */
    List<Document> findAllOwned(Collection<UUID> ids, UUID owner);

    Optional<Document> findByStorageKey(String storageKey);

    /**
     * Bytes that count towards the owner's quota: CLEAN documents, and PENDING ones requested since
     * {@code pendingSince}. Older PENDING documents were never uploaded, so they stop counting.
     */
    long quotaUsed(UUID owner, Instant pendingSince);

    /** Records a scan result; {@code sha256} only for CLEAN. */
    void markScanned(UUID id, DocumentStatus status, String sha256);
}
