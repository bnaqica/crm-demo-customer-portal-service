package com.bnaqica.crmdemocustomerportalservice.client;

import java.io.InputStream;

/** Scans file contents for malware. */
public interface VirusScanner {

    /**
     * Reads {@code content} to the end and scans it.
     *
     * @throws com.bnaqica.crmdemocustomerportalservice.exception.VirusScanException when the scan couldn't be done
     */
    boolean isInfected(InputStream content);
}
