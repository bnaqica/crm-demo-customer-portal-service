package com.bnaqica.crmdemocustomerportalservice.client;

import com.bnaqica.crmdemocustomerportalservice.config.StorageProperties;
import com.bnaqica.crmdemocustomerportalservice.dto.PresignedUpload;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** {@link DocumentStorage} on the S3 API (LocalStack locally). The bucket is private; access is only pre-signed. */
@Component
@RequiredArgsConstructor
public class S3DocumentStorage implements DocumentStorage {

    private static final int NOT_FOUND = 404;

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long size, Duration ttl) {
        var presigned = presigner.presignPutObject(request -> request.signatureDuration(ttl)
                .putObjectRequest(put -> put.bucket(properties.bucket())
                        .key(key)
                        .contentType(contentType)
                        .contentLength(size)
                        // Only the first upload succeeds, so a scanned file can't be replaced while the URL works
                        .ifNoneMatch("*")));
        Map<String, String> headers = presigned.signedHeaders().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.join(",", entry.getValue())));
        return new PresignedUpload(presigned.url().toString(), headers, presigned.expiration());
    }

    @Override
    public OptionalLong size(String key) {
        try {
            return OptionalLong.of(
                    s3.headObject(head -> head.bucket(properties.bucket()).key(key))
                            .contentLength());
        } catch (NoSuchKeyException ex) {
            return OptionalLong.empty();
        } catch (S3Exception ex) {
            if (ex.statusCode() == NOT_FOUND) {
                return OptionalLong.empty();
            }
            throw ex;
        }
    }

    @Override
    public InputStream open(String key) {
        return s3.getObject(get -> get.bucket(properties.bucket()).key(key));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(delete -> delete.bucket(properties.bucket()).key(key));
    }
}
