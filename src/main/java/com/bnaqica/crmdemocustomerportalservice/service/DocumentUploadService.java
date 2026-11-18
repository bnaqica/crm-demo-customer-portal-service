package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.PortalPrincipal;
import com.bnaqica.crmdemocustomerportalservice.dto.request.UploadRequest;
import com.bnaqica.crmdemocustomerportalservice.dto.response.DocumentResponse;
import com.bnaqica.crmdemocustomerportalservice.dto.response.UploadResponse;
import java.util.List;
import java.util.UUID;

/** The caller's own documents: requesting uploads and seeing their scan status. */
public interface DocumentUploadService {

    /** Checks type, size and the caller's quota, records a PENDING document and returns a pre-signed upload URL. */
    UploadResponse requestUpload(PortalPrincipal principal, UploadRequest request);

    /** @throws com.bnaqica.crmdemocustomerportalservice.exception.NotFoundException also for others' documents */
    DocumentResponse get(PortalPrincipal principal, UUID documentId);

    List<DocumentResponse> list(PortalPrincipal principal);
}
