package com.bnaqica.crmdemocustomerportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocustomerportalservice.entity.Submission;
import com.bnaqica.crmdemocustomerportalservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocustomerportalservice.exception.InvalidStatusEventException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class SubmissionStatusServiceImplTests {

    private final SubmissionService submissions = mock(SubmissionService.class);
    private final ProcessedEventService processed = mock(ProcessedEventService.class);
    private final SubmissionStatusService service = new SubmissionStatusServiceImpl(
            submissions, processed, JsonMapper.builder().build(), Clock.systemUTC());

    private Submission submission;
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        submission = Submission.builder()
                .id(UUID.randomUUID())
                .ownerSub(UUID.randomUUID())
                .type("CLAIM")
                .region("EU")
                .status(SubmissionStatus.SUBMITTED)
                .currentVersion(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(submissions.find(submission.getId())).thenReturn(Optional.of(submission));
        when(processed.markProcessed(eventId)).thenReturn(true);
    }

    private String event(String status, int version, String reason) {
        return """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T10:00:00Z","submissionId":"%s",\
                "version":%d,"portalUserId":"%s","status":"%s"%s}""".formatted(
                        eventId,
                        submission.getId(),
                        version,
                        submission.getOwnerSub(),
                        status,
                        reason == null ? "" : ",\"reason\":\"" + reason + "\"");
    }

    @Test
    void acceptedEventAcceptsTheCurrentVersion() {
        service.handle(event("ACCEPTED", 2, null));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
        verify(submissions).save(submission);
    }

    @Test
    void rejectedEventStoresTheReason() {
        service.handle(event("REJECTED", 2, "Missing invoice"));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(submission.getRejectionReason()).isEqualTo("Missing invoice");
    }

    @Test
    void redeliveredEventChangesNothing() {
        when(processed.markProcessed(eventId)).thenReturn(false);

        service.handle(event("ACCEPTED", 2, null));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        verify(submissions, never()).save(any());
    }

    @Test
    void eventForAnOlderVersionIsIgnored() {
        service.handle(event("REJECTED", 1, "Old"));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        verify(submissions, never()).save(any());
    }

    @Test
    void unknownSubmissionIsInvalid() {
        when(submissions.find(submission.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(event("ACCEPTED", 2, null)))
                .isInstanceOf(InvalidStatusEventException.class);
    }

    @Test
    void eventForAnotherOwnerIsInvalid() {
        String event = event("ACCEPTED", 2, null)
                .replace(submission.getOwnerSub().toString(), UUID.randomUUID().toString());

        assertThatThrownBy(() -> service.handle(event)).isInstanceOf(InvalidStatusEventException.class);
    }

    @Test
    void unknownStatusIsInvalid() {
        assertThatThrownBy(() -> service.handle(event("ROUTED", 2, null)))
                .isInstanceOf(InvalidStatusEventException.class);
    }

    @Test
    void unknownSchemaVersionIsInvalid() {
        String event = event("ACCEPTED", 2, null).replace("\"schemaVersion\":1", "\"schemaVersion\":99");

        assertThatThrownBy(() -> service.handle(event)).isInstanceOf(InvalidStatusEventException.class);
    }

    @Test
    void futureVersionIsInvalid() {
        assertThatThrownBy(() -> service.handle(event("ACCEPTED", 3, null)))
                .isInstanceOf(InvalidStatusEventException.class);
    }

    @Test
    void unreadableJsonFails() {
        assertThatThrownBy(() -> service.handle("{not json")).isInstanceOf(JacksonException.class);
    }
}
