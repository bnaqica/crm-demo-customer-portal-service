package com.bnaqica.crmdemocustomerportalservice.client;

import com.bnaqica.crmdemocustomerportalservice.dto.PresignedUpload;
import java.io.InputStream;
import java.time.Duration;
import java.util.OptionalLong;

/** The private bucket documents are uploaded to. Keys are {@code uploads/{sub}/{documentId}}. */
public interface DocumentStorage {

    /**
     * A URL that accepts one {@code PUT} of exactly {@code size} bytes of {@code contentType}, for {@code ttl}. It
     * refuses to replace an object already at {@code key}.
     */
    PresignedUpload presignUpload(String key, String contentType, long size, Duration ttl);

    /** The stored object's size, or empty when there is no object. */
    OptionalLong size(String key);

    InputStream open(String key);

    void delete(String key);
}
