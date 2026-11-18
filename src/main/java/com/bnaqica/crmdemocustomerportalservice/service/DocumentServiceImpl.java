package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.repository.DocumentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public Document create(Document document) {
        return repository.save(document);
    }

    @Override
    public Optional<Document> findOwned(UUID id, UUID owner) {
        return repository.findByIdAndOwnerSub(id, owner);
    }

    @Override
    public List<Document> listOwned(UUID owner) {
        return repository.findAllByOwnerSubOrderByCreatedAtDesc(owner);
    }

    @Override
    public List<Document> findAllOwned(Collection<UUID> ids, UUID owner) {
        return repository.findAllByIdInAndOwnerSub(ids, owner);
    }

    @Override
    public Optional<Document> findByStorageKey(String storageKey) {
        return repository.findByStorageKey(storageKey);
    }

    @Override
    public long quotaUsed(UUID owner, Instant pendingSince) {
        return repository.quotaUsed(owner, pendingSince);
    }

    @Override
    @Transactional
    public void markScanned(UUID id, DocumentStatus status, String sha256) {
        Document document = repository.findById(id).orElseThrow();
        document.setStatus(status);
        document.setSha256(sha256);
        document.setScannedAt(Instant.now(clock));
    }
}
