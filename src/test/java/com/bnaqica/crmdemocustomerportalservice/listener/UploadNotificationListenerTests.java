package com.bnaqica.crmdemocustomerportalservice.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocustomerportalservice.client.UploadQueue;
import com.bnaqica.crmdemocustomerportalservice.dto.UploadNotification;
import com.bnaqica.crmdemocustomerportalservice.exception.VirusScanException;
import com.bnaqica.crmdemocustomerportalservice.service.DocumentScanService;
import java.util.List;
import org.junit.jupiter.api.Test;

class UploadNotificationListenerTests {

    private final UploadQueue queue = mock(UploadQueue.class);
    private final DocumentScanService service = mock(DocumentScanService.class);
    private final UploadNotificationListener listener = new UploadNotificationListener(queue, service);

    private final UploadNotification handled = new UploadNotification("r1", "{\"a\":1}");
    private final UploadNotification failing = new UploadNotification("r2", "{\"b\":2}");

    @Test
    void handledNotificationIsDeletedAndAFailedOneIsKeptForRetry() {
        when(queue.receive()).thenReturn(List.of(failing, handled));
        doThrow(new VirusScanException("clamd down", null)).when(service).handleNotification(failing.body());

        listener.poll();

        verify(queue).delete(handled);
        verify(queue, never()).delete(failing);
    }

    @Test
    void unreachableQueueDoesNotStopThePoller() {
        when(queue.receive()).thenThrow(new IllegalStateException("no queue"));

        listener.poll(); // logs and returns
    }
}
