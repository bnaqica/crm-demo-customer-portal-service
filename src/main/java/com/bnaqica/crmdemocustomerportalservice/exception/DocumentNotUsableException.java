package com.bnaqica.crmdemocustomerportalservice.exception;

/** A submission names a document that isn't the caller's, or isn't scanned clean. */
public class DocumentNotUsableException extends RuntimeException {

    public DocumentNotUsableException() {
        super("Every document must be one of your uploads and scanned clean");
    }
}
