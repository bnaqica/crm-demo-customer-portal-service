package com.bnaqica.crmdemocustomerportalservice.api;

import com.bnaqica.crmdemocustomerportalservice.config.StarterFeature;
import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.request.UploadRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.response.DocumentResponse;
import com.bnaqica.crmdemocustomerportalservice.dto.response.UploadResponse;
import com.bnaqica.crmdemocustomerportalservice.service.DocumentUploadService;
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

/** The caller's documents. Files never pass through this API: they go straight to storage by pre-signed URL. */
@StarterFeature
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentUploadService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UploadResponse requestUpload(
            @Parameter(hidden = true) PortalPrincipal principal, @Valid @RequestBody UploadRequest request) {
        return service.requestUpload(principal, request);
    }

    @GetMapping
    List<DocumentResponse> list(@Parameter(hidden = true) PortalPrincipal principal) {
        return service.list(principal);
    }

    @GetMapping("/{documentId}")
    DocumentResponse get(@Parameter(hidden = true) PortalPrincipal principal, @PathVariable UUID documentId) {
        return service.get(principal, documentId);
    }
}
