package com.bnaqica.crmdemocustomerportalservice.service;

import java.util.UUID;

/** CRUD for {@code processed_event}. Joins the caller's transaction, so the mark rolls back with a failed change. */
public interface ProcessedEventService {

    /** @return true the first time for {@code eventId}, false for a redelivery */
    boolean markProcessed(UUID eventId);
}
