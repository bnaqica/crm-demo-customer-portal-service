package com.bnaqica.crmdemocustomerportalservice.service;

/** Checks and scans uploaded documents. */
public interface DocumentScanService {

    /**
     * Handles an S3 object-created notification: each PENDING document it names is size-checked and scanned, and
     * ends CLEAN, INFECTED (object deleted) or REJECTED (object deleted). Anything else in it is ignored.
     *
     * @throws com.bnaqica.crmdemocustomerportalservice.exception.VirusScanException to have it retried later
     */
    void handleNotification(String notification);
}
