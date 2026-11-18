package com.bnaqica.crmdemocustomerportalservice.exception;

/** The resource doesn't exist, or belongs to another user, which callers mustn't be able to tell apart. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what) {
        super(what + " not found");
    }
}
