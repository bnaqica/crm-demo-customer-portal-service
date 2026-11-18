package com.bnaqica.crmdemocustomerportalservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocustomerportalservice.TestcontainersConfiguration;
import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/** The quota sum against real PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DocumentRepositoryTests {

    @Autowired
    private DocumentRepository documents;

    @Autowired
    private PortalUserRepository users;

    private final Instant now = Instant.parse("2026-11-12T12:00:00Z");

    private void document(UUID owner, DocumentStatus status, long size, Instant createdAt) {
        UUID id = UUID.randomUUID();
        documents.save(Document.builder()
                .id(id)
                .ownerSub(owner)
                .storageKey("uploads/" + owner + "/" + id)
                .fileName("a.pdf")
                .contentType("application/pdf")
                .sizeBytes(size)
                .status(status)
                .createdAt(createdAt)
                .build());
    }

    /** A PENDING document whose upload URL expired long ago was never uploaded, so it stops using quota. */
    @Test
    void quotaCountsCleanAndRecentPendingDocuments() {
        UUID owner = UUID.randomUUID();
        users.insertIfAbsent(owner, "a@acme.example", now, now);
        Instant cutoff = now.minusSeconds(3600);
        document(owner, DocumentStatus.CLEAN, 100, now.minusSeconds(86_400));
        document(owner, DocumentStatus.PENDING, 20, now.minusSeconds(60));
        document(owner, DocumentStatus.PENDING, 3, now.minusSeconds(7200)); // abandoned
        document(owner, DocumentStatus.INFECTED, 4000, now);

        assertThat(documents.quotaUsed(owner, cutoff)).isEqualTo(120);
    }
}
