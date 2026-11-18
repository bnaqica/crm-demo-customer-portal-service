package com.bnaqica.crmdemocustomerportalservice.exception;

/** An upload request breaks a type, size or quota rule. */
public class UploadNotAllowedException extends RuntimeException {

    public UploadNotAllowedException(String message) {
        super(message);
    }
}
