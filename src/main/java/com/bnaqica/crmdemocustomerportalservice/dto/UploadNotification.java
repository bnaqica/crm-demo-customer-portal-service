package com.bnaqica.crmdemocustomerportalservice.dto;

/**
 * A message from the upload queue.
 *
 * @param receipt what the queue needs to delete the message once it's handled
 * @param body the S3 event notification, as JSON
 */
public record UploadNotification(String receipt, String body) {}
