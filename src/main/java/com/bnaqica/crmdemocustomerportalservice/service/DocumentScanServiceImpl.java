package com.bnaqica.crmdemocustomerportalservice.service;

import com.bnaqica.crmdemocustomerportalservice.client.DocumentStorage;
import com.bnaqica.crmdemocustomerportalservice.client.VirusScanner;
import com.bnaqica.crmdemocustomerportalservice.config.UploadProperties;
import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.exception.VirusScanException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentScanServiceImpl implements DocumentScanService {

    private final DocumentService documents;
    private final DocumentStorage storage;
    private final VirusScanner scanner;
    private final UploadProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public void handleNotification(String notification) {
        JsonNode records;
        try {
            records = jsonMapper.readTree(notification).path("Records");
        } catch (JacksonException ex) {
            log.error("Ignoring an unreadable upload notification", ex); // retrying wouldn't help
            return;
        }
        for (JsonNode record : records) {
            if (record.path("eventName").asString("").startsWith("ObjectCreated")) {
                String key = URLDecoder.decode(
                        record.path("s3").path("object").path("key").asString(""), StandardCharsets.UTF_8);
                documents
                        .findByStorageKey(key)
                        .ifPresentOrElse(this::check, () -> log.debug("No document for {}", key));
            }
        }
    }

    private void check(Document document) {
        if (document.getStatus() != DocumentStatus.PENDING) {
            log.debug("Document {} is already {}", document.getId(), document.getStatus());
            return;
        }
        OptionalLong size = storage.size(document.getStorageKey());
        if (size.isEmpty()) {
            log.debug("Object of document {} is gone", document.getId());
            return;
        }
        if (size.getAsLong() > document.getSizeBytes()
                || size.getAsLong() > properties.maxFileSize().toBytes()) {
            reject(document, DocumentStatus.REJECTED);
            log.info("Rejected document {}: object is larger than declared", document.getId());
            return;
        }
        MessageDigest digest = sha256();
        boolean infected;
        try (InputStream content = new DigestInputStream(storage.open(document.getStorageKey()), digest)) {
            infected = scanner.isInfected(content);
        } catch (IOException ex) {
            throw new VirusScanException("Couldn't read document " + document.getId(), ex);
        }
        if (infected) {
            reject(document, DocumentStatus.INFECTED);
            log.info("Document {} is infected; deleted it", document.getId());
        } else {
            documents.markScanned(
                    document.getId(), DocumentStatus.CLEAN, HexFormat.of().formatHex(digest.digest()));
            log.info("Document {} is clean", document.getId());
        }
    }

    private void reject(Document document, DocumentStatus status) {
        storage.delete(document.getStorageKey());
        documents.markScanned(document.getId(), status, null);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Every JVM has SHA-256", ex);
        }
    }
}
