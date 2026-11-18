package com.bnaqica.crmdemocustomerportalservice.listener;

import com.bnaqica.crmdemocustomerportalservice.service.SubmissionStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Hands each message on {@code submission-status} to {@link SubmissionStatusService}. */
@Component
@RequiredArgsConstructor
public class SubmissionStatusListener {

    private final SubmissionStatusService service;

    @KafkaListener(topics = "${portal.topics.submission-status}")
    void on(String payload) {
        service.handle(payload);
    }
}
