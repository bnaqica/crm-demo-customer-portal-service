package com.bnaqica.crmdemocustomerportalservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocustomerportalservice.client.DocumentStorage;
import com.bnaqica.crmdemocustomerportalservice.client.VirusScanner;
import com.bnaqica.crmdemocustomerportalservice.config.UploadProperties;
import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

class DocumentScanServiceImplTests {

    private static final byte[] CONTENT = "%PDF-1.7 test".getBytes(StandardCharsets.UTF_8);

    private final DocumentService documents = mock(DocumentService.class);
    private final DocumentStorage storage = mock(DocumentStorage.class);
    private final VirusScanner scanner = mock(VirusScanner.class);
    private final UploadProperties properties = new UploadProperties(
            Set.of("application/pdf"),
            DataSize.ofMegabytes(10),
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            new UploadProperties.Quota(DataSize.ofMegabytes(20), DataSize.ofMegabytes(200)));
    private final DocumentScanService service = new DocumentScanServiceImpl(
            documents, storage, scanner, properties, JsonMapper.builder().build());

    private Document pending(long declaredSize) {
        UUID id = UUID.randomUUID();
        Document document = Document.builder()
                .id(id)
                .ownerSub(UUID.randomUUID())
                .storageKey("uploads/u/" + id)
                .fileName("a.pdf")
                .contentType("application/pdf")
                .sizeBytes(declaredSize)
                .status(DocumentStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        when(documents.findByStorageKey(document.getStorageKey())).thenReturn(Optional.of(document));
        return document;
    }

    private static String notification(String key) {
        return """
                {"Records":[{"eventName":"ObjectCreated:Put","s3":{"bucket":{"name":"portal-documents"},\
                "object":{"key":"%s","size":13}}}]}""".formatted(key);
    }

    private void stored(Document document, long size) {
        when(storage.size(document.getStorageKey())).thenReturn(OptionalLong.of(size));
        when(storage.open(document.getStorageKey())).thenReturn(new ByteArrayInputStream(CONTENT));
    }

    @Test
    void cleanObjectIsMarkedCleanWithItsSha256() throws Exception {
        Document document = pending(CONTENT.length);
        stored(document, CONTENT.length);
        when(scanner.isInfected(any(InputStream.class))).thenAnswer(call -> {
            call.getArgument(0, InputStream.class).readAllBytes();
            return false;
        });

        service.handleNotification(notification(document.getStorageKey()));

        String expected = HexFormat.of()
                .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(CONTENT));
        verify(documents).markScanned(document.getId(), DocumentStatus.CLEAN, expected);
        verify(storage, never()).delete(anyString());
    }

    @Test
    void infectedObjectIsDeletedAndMarkedInfected() {
        Document document = pending(CONTENT.length);
        stored(document, CONTENT.length);
        when(scanner.isInfected(any(InputStream.class))).thenReturn(true);

        service.handleNotification(notification(document.getStorageKey()));

        verify(storage).delete(document.getStorageKey());
        verify(documents).markScanned(eq(document.getId()), eq(DocumentStatus.INFECTED), isNull());
    }

    @Test
    void objectLargerThanDeclaredIsDeletedUnscanned() {
        Document document = pending(5);
        stored(document, CONTENT.length);

        service.handleNotification(notification(document.getStorageKey()));

        verify(storage).delete(document.getStorageKey());
        verify(documents).markScanned(eq(document.getId()), eq(DocumentStatus.REJECTED), isNull());
        verify(scanner, never()).isInfected(any());
    }

    @Test
    void alreadyScannedDocumentIsLeftAlone() {
        Document document = pending(CONTENT.length);
        document.setStatus(DocumentStatus.CLEAN);

        service.handleNotification(notification(document.getStorageKey()));

        verify(scanner, never()).isInfected(any());
        verify(documents, never()).markScanned(any(), any(), any());
    }

    @Test
    void keysAreUrlDecoded() {
        Document document = pending(CONTENT.length);
        stored(document, CONTENT.length);
        when(scanner.isInfected(any(InputStream.class))).thenReturn(true);

        service.handleNotification(notification(document.getStorageKey().replace("/", "%2F")));

        verify(documents).markScanned(eq(document.getId()), eq(DocumentStatus.INFECTED), isNull());
    }

    @Test
    void s3TestEventAndUnknownKeysAreIgnored() {
        service.handleNotification("{\"Service\":\"Amazon S3\",\"Event\":\"s3:TestEvent\"}");
        service.handleNotification(notification("uploads/u/" + UUID.randomUUID()));

        ArgumentCaptor<UUID> ids = ArgumentCaptor.forClass(UUID.class);
        verify(documents, never()).markScanned(ids.capture(), any(), any());
        assertThat(ids.getAllValues()).isEqualTo(List.of());
    }
}
