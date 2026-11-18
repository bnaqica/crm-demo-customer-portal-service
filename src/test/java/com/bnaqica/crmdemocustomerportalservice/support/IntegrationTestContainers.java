package com.bnaqica.crmdemocustomerportalservice.support;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Real PostgreSQL, Kafka, Keycloak (with the test realms from {@code src/test/resources/keycloak}), LocalStack
 * (S3 and SQS) and ClamAV. Import into
 * {@code @SpringBootTest} classes that need real tokens or Kafka; Spring caches the context, so the containers start
 * once per test run. The service trusts the {@code portal} realm, as in production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestContainers {

    static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.8.1"; // same as local-docker
    static final String CLAMAV_IMAGE = "clamav/clamav:1.4";
    static final String LOCALSTACK_IMAGE = "localstack/localstack:4.4";
    static final String[] REALMS = {"crm-admin", "crm-internal", "portal"};

    /** Generous, because containers start slowly on a busy machine. */
    static final Duration STARTUP = Duration.ofMinutes(8);

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18")).withStartupTimeout(STARTUP);
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2")).withStartupTimeout(STARTUP);
    }

    @Bean
    KeycloakContainer keycloakContainer() {
        return new KeycloakContainer();
    }

    @Bean
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse(LOCALSTACK_IMAGE))
                .withServices("s3", "sqs")
                .withStartupTimeout(STARTUP);
    }

    /** Creates the bucket, the upload queue and the bucket's notifications to it, as local-docker must. */
    @Bean
    DynamicPropertyRegistrar storageProperties(LocalStackContainer localStack) {
        localStack.start();
        String queueUrl = TestStorage.prepare(localStack);
        return registry -> {
            registry.add(
                    "portal.storage.endpoint", () -> localStack.getEndpoint().toString());
            registry.add("portal.storage.region", localStack::getRegion);
            registry.add("portal.storage.access-key", localStack::getAccessKey);
            registry.add("portal.storage.secret-key", localStack::getSecretKey);
            registry.add("portal.storage.bucket", () -> TestStorage.BUCKET);
            registry.add("portal.storage.upload-queue-url", () -> queueUrl);
        };
    }

    @Bean
    ClamAvContainer clamAvContainer() {
        return new ClamAvContainer();
    }

    @Bean
    DynamicPropertyRegistrar clamAvProperties(ClamAvContainer clamAv) {
        return registry -> {
            registry.add("portal.clamav.host", clamAv::getHost);
            registry.add("portal.clamav.port", () -> clamAv.getMappedPort(ClamAvContainer.PORT));
        };
    }

    @Bean
    DynamicPropertyRegistrar keycloakProperties(KeycloakContainer keycloak) {
        return registry ->
                registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloak.issuer("portal"));
    }

    /** ClamAV's {@code clamd}. The image ships its signatures, but loading them takes a while. */
    public static class ClamAvContainer extends GenericContainer<ClamAvContainer> {

        static final int PORT = 3310;

        ClamAvContainer() {
            super(DockerImageName.parse(CLAMAV_IMAGE));
            withExposedPorts(PORT);
            waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP));
        }
    }

    public static class KeycloakContainer extends GenericContainer<KeycloakContainer> {

        KeycloakContainer() {
            super(DockerImageName.parse(KEYCLOAK_IMAGE));
            withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
            withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
            withEnv("KC_HEALTH_ENABLED", "true");
            for (String realm : REALMS) {
                withCopyFileToContainer(
                        MountableFile.forClasspathResource("keycloak/" + realm + "-realm.json"),
                        "/opt/keycloak/data/import/" + realm + "-realm.json");
            }
            withCommand("start-dev", "--import-realm");
            withExposedPorts(8080, 9000);
            waitingFor(Wait.forHttp("/health/ready").forPort(9000).withStartupTimeout(STARTUP));
        }

        /** The master realm demands HTTPS from outside the container; tests use its admin over plain HTTP. */
        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            try {
                String kcadm = "/opt/keycloak/bin/kcadm.sh";
                execInContainer(
                        kcadm,
                        "config",
                        "credentials",
                        "--server",
                        "http://localhost:8080",
                        "--realm",
                        "master",
                        "--user",
                        "admin",
                        "--password",
                        "admin");
                execInContainer(kcadm, "update", "realms/master", "-s", "sslRequired=NONE");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        public String baseUrl() {
            return "http://" + getHost() + ":" + getMappedPort(8080);
        }

        public String issuer(String realm) {
            return baseUrl() + "/realms/" + realm;
        }

        public String tokenUri(String realm) {
            return issuer(realm) + "/protocol/openid-connect/token";
        }
    }
}
