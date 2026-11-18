package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.request.CorrectionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.request.SubmissionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocustomerportalservice.dto.response.SubmissionVersionResponse;
import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionVersion;
import com.bnaqica.crmdemocustomerportalservice.exception.DocumentNotUsableException;
import com.bnaqica.crmdemocustomerportalservice.exception.NotFoundException;
import com.bnaqica.crmdemocustomerportalservice.publisher.PortalIntakePublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSubmissionServiceImpl implements PortalSubmissionService {

    private static final TypeReference<Map<String, Object>> DATA = new TypeReference<>() {};

    private final SubmissionService submissions;
    private final DocumentService documents;
    private final PortalIntakePublisher intake;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    @Transactional
    public SubmissionResponse create(PortalPrincipal principal, SubmissionRequest request) {
        Set<Document> attached = usableDocuments(principal, request.documentIds());
        Instant now = Instant.now(clock);
        Submission submission = submissions.save(Submission.builder()
                .id(UUID.randomUUID())
                .ownerSub(principal.sub())
                .type(request.type())
                .region(request.region())
                .status(SubmissionStatus.SUBMITTED)
                .currentVersion(1)
                .createdAt(now)
                .updatedAt(now)
                .build());
        SubmissionVersion version = addVersion(submission, request.data(), attached, now);
        intake.submissionReceived(principal.email(), submission, version);
        log.info("Created submission {}", submission.getId());
        return response(submission, List.of(version));
    }

    @Override
    @Transactional
    public SubmissionResponse correct(PortalPrincipal principal, UUID submissionId, CorrectionRequest request) {
        Submission submission = submissions
                .lockOwned(submissionId, principal.sub()) // one correction at a time, so versions don't collide
                .orElseThrow(() -> new NotFoundException("Submission"));
        Set<Document> attached = usableDocuments(principal, request.documentIds());
        Instant now = Instant.now(clock);
        submission.setCurrentVersion(submission.getCurrentVersion() + 1);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setRejectionReason(null);
        submission.setUpdatedAt(now);
        submissions.save(submission);
        SubmissionVersion version = addVersion(submission, request.data(), attached, now);
        intake.submissionReceived(principal.email(), submission, version);
        log.info("Corrected submission {} as version {}", submission.getId(), version.getVersion());
        return response(submission, submissions.versions(submission.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse get(PortalPrincipal principal, UUID submissionId) {
        Submission submission = owned(principal, submissionId);
        return response(submission, submissions.versions(submission.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> list(PortalPrincipal principal) {
        return submissions.listOwned(principal.sub()).stream()
                .map(submission -> response(submission, List.of()))
                .toList();
    }

    private Submission owned(PortalPrincipal principal, UUID submissionId) {
        return submissions
                .findOwned(submissionId, principal.sub())
                .orElseThrow(() -> new NotFoundException("Submission"));
    }

    /** The caller's CLEAN documents for {@code ids}, or 422 if any is missing, someone else's, or not clean. */
    private Set<Document> usableDocuments(PortalPrincipal principal, List<UUID> ids) {
        Set<UUID> wanted = ids == null ? Set.of() : new HashSet<>(ids);
        if (wanted.isEmpty()) {
            return Set.of();
        }
        List<Document> found = documents.findAllOwned(wanted, principal.sub());
        if (found.size() != wanted.size()
                || found.stream().anyMatch(document -> document.getStatus() != DocumentStatus.CLEAN)) {
            throw new DocumentNotUsableException();
        }
        return new HashSet<>(found);
    }

    private SubmissionVersion addVersion(
            Submission submission, Map<String, Object> data, Set<Document> attached, Instant now) {
        return submissions.addVersion(SubmissionVersion.builder()
                .id(UUID.randomUUID())
                .submissionId(submission.getId())
                .version(submission.getCurrentVersion())
                .data(jsonMapper.writeValueAsString(data))
                .documents(attached)
                .createdAt(now)
                .build());
    }

    private SubmissionResponse response(Submission submission, List<SubmissionVersion> versions) {
        return SubmissionResponse.builder()
                .submissionId(submission.getId())
                .type(submission.getType())
                .region(submission.getRegion())
                .status(submission.getStatus())
                .rejectionReason(submission.getRejectionReason())
                .version(submission.getCurrentVersion())
                .createdAt(submission.getCreatedAt())
                .updatedAt(submission.getUpdatedAt())
                .versions(versions.stream()
                        .sorted(Comparator.comparingInt(SubmissionVersion::getVersion))
                        .map(this::versionResponse)
                        .toList())
                .build();
    }

    private SubmissionVersionResponse versionResponse(SubmissionVersion version) {
        return SubmissionVersionResponse.builder()
                .version(version.getVersion())
                .data(jsonMapper.readValue(version.getData(), DATA))
                .documentIds(version.getDocuments().stream()
                        .map(Document::getId)
                        .sorted()
                        .toList())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
