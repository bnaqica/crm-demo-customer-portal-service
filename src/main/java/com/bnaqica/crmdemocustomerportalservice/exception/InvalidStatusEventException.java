package com.bnaqica.crmdemocustomerportalservice.exception;

/** A status event that can never be applied, such as one for an unknown submission. It goes to the DLQ at once. */
public class InvalidStatusEventException extends RuntimeException {

    public InvalidStatusEventException(String message) {
        super(message);
    }
}
