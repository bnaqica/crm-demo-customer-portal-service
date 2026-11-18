package com.bnaqica.crmdemocustomerportalservice.exception;

/** The virus scanner couldn't scan a document; the upload notification is retried later. */
public class VirusScanException extends RuntimeException {

    public VirusScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
