package com.bnaqica.crmdemocustomerportalservice.client;

import com.bnaqica.crmdemocustomerportalservice.config.StorageProperties;
import com.bnaqica.crmdemocustomerportalservice.dto.UploadNotification;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

/** {@link UploadQueue} on SQS (LocalStack locally), with long polling. */
@Component
@RequiredArgsConstructor
public class SqsUploadQueue implements UploadQueue {

    private static final int WAIT_SECONDS = 5;
    private static final int MAX_MESSAGES = 10;

    private final SqsClient sqs;
    private final StorageProperties properties;

    @Override
    public List<UploadNotification> receive() {
        return sqs
                .receiveMessage(receive -> receive.queueUrl(properties.uploadQueueUrl())
                        .waitTimeSeconds(WAIT_SECONDS)
                        .maxNumberOfMessages(MAX_MESSAGES))
                .messages()
                .stream()
                .map(message -> new UploadNotification(message.receiptHandle(), message.body()))
                .toList();
    }

    @Override
    public void delete(UploadNotification notification) {
        sqs.deleteMessage(delete -> delete.queueUrl(properties.uploadQueueUrl()).receiptHandle(notification.receipt()));
    }
}
