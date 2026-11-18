package com.bnaqica.crmdemocustomerportalservice.client;

import com.bnaqica.crmdemocustomerportalservice.dto.UploadNotification;
import java.util.List;

/** The queue the document bucket sends object-created notifications to. */
public interface UploadQueue {

    /** Waits briefly for messages; a message not deleted comes back after the queue's visibility timeout. */
    List<UploadNotification> receive();

    void delete(UploadNotification notification);
}
