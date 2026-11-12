package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.dto.ApiAuditEntry;

public interface ApiAuditService {

    void record(ApiAuditEntry entry);
}
