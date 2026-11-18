package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.client.DocumentStorage;
import com.bnaqica.crmdemocustomerportalservice.config.UploadProperties;
import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.PresignedUpload;
import com.bnaqica.crmdemocustomerportalservice.dto.Tier;
import com.bnaqica.crmdemocustomerportalservice.dto.request.UploadRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.response.DocumentResponse;
import com.bnaqica.crmdemocustomerportalservice.dto.response.UploadResponse;
import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.exception.NotFoundException;
import com.bnaqica.crmdemocustomerportalservice.exception.UploadNotAllowedException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadServiceImpl implements DocumentUploadService {

    private final DocumentService documents;
    private final PortalUserService users;
    private final DocumentStorage storage;
    private final UploadProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public UploadResponse requestUpload(PortalPrincipal principal, UploadRequest request) {
        if (!properties.allowedContentTypes().contains(request.contentType())) {
            throw new UploadNotAllowedException("Files of type " + request.contentType() + " can't be uploaded");
        }
        if (request.size() > properties.maxFileSize().toBytes()) {
            throw new UploadNotAllowedException("Files can be at most " + properties.maxFileSize());
        }
        users.lock(principal.sub()); // one quota check at a time per user
        long quota = quota(principal.tier());
        // A PENDING document counts while its upload URL works, and for a grace period for the scan after that
        Instant pendingSince = Instant.now(clock).minus(properties.urlTtl()).minus(properties.scanGrace());
        if (documents.quotaUsed(principal.sub(), pendingSince) + request.size() > quota) {
            throw new UploadNotAllowedException("This upload would exceed your storage quota");
        }
        UUID id = UUID.randomUUID();
        String key = "uploads/" + principal.sub() + "/" + id;
        Document document = documents.create(Document.builder()
                .id(id)
                .ownerSub(principal.sub())
                .storageKey(key)
                .fileName(request.fileName())
                .contentType(request.contentType())
                .sizeBytes(request.size())
                .status(DocumentStatus.PENDING)
                .createdAt(Instant.now(clock))
                .build());
        PresignedUpload upload = storage.presignUpload(key, request.contentType(), request.size(), properties.urlTtl());
        log.info("Issued upload URL for document {}", id);
        return UploadResponse.builder()
                .documentId(document.getId())
                .status(document.getStatus())
                .uploadUrl(upload.url())
                .uploadHeaders(upload.headers())
                .expiresAt(upload.expiresAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse get(PortalPrincipal principal, UUID documentId) {
        return documents
                .findOwned(documentId, principal.sub())
                .map(DocumentUploadServiceImpl::toResponse)
                .orElseThrow(() -> new NotFoundException("Document"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> list(PortalPrincipal principal) {
        return documents.listOwned(principal.sub()).stream()
                .map(DocumentUploadServiceImpl::toResponse)
                .toList();
    }

    private long quota(Tier tier) {
        return (tier == Tier.STANDARD
                        ? properties.quota().standard()
                        : properties.quota().starter())
                .toBytes();
    }

    private static DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .size(document.getSizeBytes())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
