package com.bnaqica.crmdemocustomerportalservice.listener;

import com.bnaqica.crmdemocustomerportalservice.client.UploadQueue;
import com.bnaqica.crmdemocustomerportalservice.dto.UploadNotification;
import com.bnaqica.crmdemocustomerportalservice.service.DocumentScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the upload queue and hands each S3 notification to {@link DocumentScanService}. A notification is deleted
 * only once it's handled, so one that fails comes back after the queue's visibility timeout.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadNotificationListener {

    private final UploadQueue queue;
    private final DocumentScanService service;

    @Scheduled(fixedDelayString = "${portal.uploads.poll-interval:1s}")
    public void poll() {
        try {
            for (UploadNotification notification : queue.receive()) {
                handle(notification);
            }
        } catch (RuntimeException ex) {
            log.warn("Couldn't read the upload queue", ex);
        }
    }

    private void handle(UploadNotification notification) {
        try {
            service.handleNotification(notification.body());
            queue.delete(notification);
        } catch (RuntimeException ex) {
            log.error("Couldn't handle an upload notification; it will be retried", ex);
        }
    }
}
