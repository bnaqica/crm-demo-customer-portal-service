package com.bnaqica.crmdemocustomerportalservice.exception;

/** An endpoint needs the calling portal user but the request has no token (only possible in the local profile). */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("This endpoint needs a portal user token");
    }
}
