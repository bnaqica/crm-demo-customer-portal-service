package com.bnaqica.crmdemocustomerportalservice.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.testcontainers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/** Test-only S3 and SQS setup and checks against LocalStack. */
public final class TestStorage {

    public static final String BUCKET = "portal-documents";
    public static final String QUEUE = "portal-uploads";

    private TestStorage() {}

    /** Creates the private bucket, the upload queue, and the bucket's object-created notifications; returns the queue URL. */
    static String prepare(LocalStackContainer localStack) {
        try (S3Client s3 = s3(localStack);
                SqsClient sqs = SqsClient.builder()
                        .endpointOverride(localStack.getEndpoint())
                        .region(Region.of(localStack.getRegion()))
                        .credentialsProvider(credentials(localStack))
                        .build()) {
            s3.createBucket(b -> b.bucket(BUCKET));
            String queueUrl = sqs.createQueue(q -> q.queueName(QUEUE)).queueUrl();
            String queueArn = sqs.getQueueAttributes(
                            a -> a.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
                    .attributes()
                    .get(QueueAttributeName.QUEUE_ARN);
            s3.putBucketNotificationConfiguration(n -> n.bucket(BUCKET)
                    .notificationConfiguration(c -> c.queueConfigurations(QueueConfiguration.builder()
                            .queueArn(queueArn)
                            .events(Event.S3_OBJECT_CREATED)
                            .build())));
            return queueUrl;
        }
    }

    public static boolean exists(LocalStackContainer localStack, String key) {
        try (S3Client s3 = s3(localStack)) {
            s3.headObject(h -> h.bucket(BUCKET).key(key));
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw ex;
        }
    }

    /** Uploads {@code body} to a pre-signed URL as a browser would; returns the HTTP status. */
    public static int put(String url, Map<String, String> headers, byte[] body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach((name, value) -> {
            if (!name.equalsIgnoreCase("Content-Length") && !name.equalsIgnoreCase("Host")) {
                request.header(name, value);
            }
        });
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        }
    }

    private static S3Client s3(LocalStackContainer localStack) {
        return S3Client.builder()
                .endpointOverride(localStack.getEndpoint())
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(credentials(localStack))
                .forcePathStyle(true)
                .build();
    }

    private static StaticCredentialsProvider credentials(LocalStackContainer localStack) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()));
    }
}
