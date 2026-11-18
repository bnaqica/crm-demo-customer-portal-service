package com.bnaqica.crmdemocustomerportalservice.api;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.request.CorrectionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.request.SubmissionRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocustomerportalservice.service.PortalSubmissionService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own submissions. A standard feature. Submitted versions are never edited; corrections add one. */
@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final PortalSubmissionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SubmissionResponse create(
            @Parameter(hidden = true) PortalPrincipal principal, @Valid @RequestBody SubmissionRequest request) {
        return service.create(principal, request);
    }

    @GetMapping
    List<SubmissionResponse> list(@Parameter(hidden = true) PortalPrincipal principal) {
        return service.list(principal);
    }

    @GetMapping("/{submissionId}")
    SubmissionResponse get(@Parameter(hidden = true) PortalPrincipal principal, @PathVariable UUID submissionId) {
        return service.get(principal, submissionId);
    }

    @PostMapping("/{submissionId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    SubmissionResponse correct(
            @Parameter(hidden = true) PortalPrincipal principal,
            @PathVariable UUID submissionId,
            @Valid @RequestBody CorrectionRequest request) {
        return service.correct(principal, submissionId, request);
    }
}
