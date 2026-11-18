package com.bnaqica.crmdemocustomerportalservice.service;

/** Applies the CRM's decisions, from {@code SubmissionAccepted} and {@code SubmissionRejected} v1 events. */
public interface SubmissionStatusService {

    /**
     * Applies one status event, once per event id. An event for an older version (overtaken by a correction) is
     * ignored.
     *
     * @throws com.bnaqica.crmdemocustomerportalservice.exception.InvalidStatusEventException for an event that can
     *     never be applied: wrong schema version or status, unknown submission, another owner, a future version
     * @throws tools.jackson.core.JacksonException for unreadable JSON
     */
    void handle(String payload);
}
