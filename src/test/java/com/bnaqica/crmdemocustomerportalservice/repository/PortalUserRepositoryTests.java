package com.bnaqica.crmdemocustomerportalservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocustomerportalservice.TestcontainersConfiguration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/** The race-safe upsert and email update against real PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PortalUserRepositoryTests {

    @Autowired
    private PortalUserRepository repository;

    private final Instant now = Instant.parse("2026-11-11T10:00:00Z");
    private final Instant issued = Instant.parse("2026-11-11T09:59:00Z");

    @Test
    void insertIfAbsentCreatesOnceOnly() {
        UUID sub = UUID.randomUUID();

        assertThat(repository.insertIfAbsent(sub, "a@acme.example", issued, now))
                .isEqualTo(1);
        assertThat(repository.insertIfAbsent(sub, "b@acme.example", issued, now))
                .isZero();

        assertThat(repository.findById(sub).orElseThrow().getEmail()).isEqualTo("a@acme.example");
    }

    @Test
    void updateEmailChangesOnlyADifferentEmail() {
        UUID sub = UUID.randomUUID();
        repository.insertIfAbsent(sub, "old@acme.example", issued, now);
        Instant later = issued.plusSeconds(60);

        assertThat(repository.updateEmailIfChanged(sub, "old@acme.example", later, now))
                .isZero();
        assertThat(repository.updateEmailIfChanged(sub, "new@acme.example", later, now))
                .isEqualTo(1);

        assertThat(repository.findById(sub).orElseThrow().getEmail()).isEqualTo("new@acme.example");
    }

    /** {@code iat} has one-second resolution, so a new token can share the stored token's second. */
    @Test
    void tokenFromTheSameSecondChangesTheEmail() {
        UUID sub = UUID.randomUUID();
        repository.insertIfAbsent(sub, "old@acme.example", issued, now);

        assertThat(repository.updateEmailIfChanged(sub, "new@acme.example", issued, now))
                .isEqualTo(1);
    }

    /** A token issued before the one that set the stored email mustn't set it back. */
    @Test
    void olderTokenDoesNotChangeTheEmailBack() {
        UUID sub = UUID.randomUUID();
        repository.insertIfAbsent(sub, "old@acme.example", issued, now);
        repository.updateEmailIfChanged(sub, "new@acme.example", issued.plusSeconds(60), now);

        assertThat(repository.updateEmailIfChanged(sub, "old@acme.example", issued.plusSeconds(30), now))
                .isZero();

        assertThat(repository.findById(sub).orElseThrow().getEmail()).isEqualTo("new@acme.example");
    }
}
