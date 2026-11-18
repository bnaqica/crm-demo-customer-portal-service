package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocustomerportalservice.exception.InvalidStatusEventException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionStatusServiceImpl implements SubmissionStatusService {

    static final int SCHEMA_VERSION = 1;

    private final SubmissionService submissions;
    private final ProcessedEventService processed;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    @Transactional
    public void handle(String payload) {
        JsonNode event = jsonMapper.readTree(payload);
        if (event.path("schemaVersion").asInt(-1) != SCHEMA_VERSION) {
            throw new InvalidStatusEventException("Unsupported schema version " + event.path("schemaVersion"));
        }
        UUID eventId = uuid(event, "eventId");
        UUID submissionId = uuid(event, "submissionId");
        UUID portalUserId = uuid(event, "portalUserId");
        SubmissionStatus status = status(event.path("status").asString(""));
        int version = event.path("version").asInt(0);
        if (!processed.markProcessed(eventId)) {
            log.debug("Status event {} was already applied", eventId);
            return;
        }
        Submission submission = submissions
                .find(submissionId)
                .filter(found -> found.getOwnerSub().equals(portalUserId))
                .orElseThrow(() -> new InvalidStatusEventException("Unknown submission " + submissionId));
        if (version > submission.getCurrentVersion() || version < 1) {
            throw new InvalidStatusEventException("Submission " + submissionId + " has no version " + version);
        }
        if (version < submission.getCurrentVersion()) {
            log.debug("Ignoring status of submission {} version {}: corrected since", submissionId, version);
            return;
        }
        submission.setStatus(status);
        submission.setRejectionReason(
                status == SubmissionStatus.REJECTED ? event.path("reason").asString(null) : null);
        submission.setUpdatedAt(Instant.now(clock));
        submissions.save(submission);
        log.info("Submission {} version {} is {}", submissionId, version, status);
    }

    private static UUID uuid(JsonNode event, String field) {
        try {
            return UUID.fromString(event.path(field).asString(""));
        } catch (IllegalArgumentException ex) {
            throw new InvalidStatusEventException("Missing or invalid " + field);
        }
    }

    private static SubmissionStatus status(String value) {
        return switch (value) {
            case "ACCEPTED" -> SubmissionStatus.ACCEPTED;
            case "REJECTED" -> SubmissionStatus.REJECTED;
            default -> throw new InvalidStatusEventException("Unknown status " + value);
        };
    }
}
