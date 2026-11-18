package com.bnaqica.crmdemocustomerportalservice.entity;

/** Where a document is in the upload and scan flow. Only CLEAN documents can be used in a submission. */
public enum DocumentStatus {
    /** Upload URL issued; the object may not be there yet, and isn't scanned. */
    PENDING,
    CLEAN,
    /** The scanner found malware; the object was deleted. */
    INFECTED,
    /** The uploaded object didn't match what was declared (for example it was larger); the object was deleted. */
    REJECTED
}
