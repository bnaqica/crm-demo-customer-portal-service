package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.request.CorrectionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.request.SubmissionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.response.SubmissionResponse;
import java.util.List;
import java.util.UUID;

/**
 * The caller's own submissions. Creating or correcting one stores {@code SubmissionReceived} in the same
 * transaction. Another user's submission is reported as not found.
 */
public interface PortalSubmissionService {

    SubmissionResponse create(PortalPrincipal principal, SubmissionRequest request);

    /** Version 1, then one more for each correction; earlier versions never change. */
    SubmissionResponse correct(PortalPrincipal principal, UUID submissionId, CorrectionRequest request);

    SubmissionResponse get(PortalPrincipal principal, UUID submissionId);

    /** Newest first, without versions. */
    List<SubmissionResponse> list(PortalPrincipal principal);
}
