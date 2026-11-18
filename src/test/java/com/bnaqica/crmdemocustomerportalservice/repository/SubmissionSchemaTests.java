package com.bnaqica.crmdemocustomerportalservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnaqica.crmdemocustomerportalservice.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** V3 migration: the submission tables exist in the service schema and protect version numbers. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SubmissionSchemaTests {

    private static final String SCHEMA = "crm_demo_customer_portal_service";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void submissionTablesExistInTheServiceSchema() {
        var tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ?", String.class, SCHEMA);

        assertThat(tables)
                .contains(
                        "submission",
                        "submission_version",
                        "submission_version_document",
                        "document",
                        "processed_event");
    }

    @Test
    void aVersionNumberIsUsedOncePerSubmission() {
        UUID sub = UUID.randomUUID();
        UUID submission = UUID.randomUUID();
        jdbc.update("INSERT INTO " + SCHEMA + ".portal_user VALUES (?, 'a@acme.example', now(), now(), now())", sub);
        jdbc.update(
                "INSERT INTO " + SCHEMA + ".submission (id, owner_sub, type, region, status, current_version,"
                        + " created_at, updated_at) VALUES (?, ?, 'CLAIM', 'EU', 'SUBMITTED', 1, now(), now())",
                submission,
                sub);
        String version = "INSERT INTO " + SCHEMA + ".submission_version (id, submission_id, version, data, created_at)"
                + " VALUES (?, ?, 1, '{}', now())";
        jdbc.update(version, UUID.randomUUID(), submission);

        assertThatThrownBy(() -> jdbc.update(version, UUID.randomUUID(), submission))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
