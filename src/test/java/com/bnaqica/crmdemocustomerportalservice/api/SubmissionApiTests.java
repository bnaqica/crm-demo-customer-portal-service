package com.bnaqica.crmdemocustomerportalservice.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.repository.DocumentRepository;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.PortalApi;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** portal-submissions spec: create, owner only, and corrections, with real tokens and database. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class SubmissionApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    @Autowired
    private DocumentRepository documents;

    private PortalApi standard;

    @BeforeEach
    void setUp() throws Exception {
        standard = new PortalApi(mockMvc, keycloak, documents, "external-standard");
    }

    private static Map<String, Object> submission(List<UUID> documentIds) {
        return Map.of("type", "CLAIM", "region", "EU", "data", Map.of("amount", 120), "documentIds", documentIds);
    }

    @Test
    void standardUserCreatesASubmittedSubmission() throws Exception {
        UUID document = standard.document(DocumentStatus.CLEAN);

        String id = PortalApi.field(
                standard.post("/submissions", submission(List.of(document)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("SUBMITTED"))
                        .andExpect(jsonPath("$.type").value("CLAIM"))
                        .andExpect(jsonPath("$.region").value("EU"))
                        .andExpect(jsonPath("$.version").value(1))
                        .andExpect(jsonPath("$.versions[0].documentIds", contains(document.toString()))),
                "submissionId");

        standard.get("/submissions/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.versions[0].data.amount").value(120));
        standard.get("/submissions").andExpect(status().isOk()).andExpect(jsonPath("$[*].submissionId", contains(id)));
    }

    @Test
    void unscannedDocumentIsRejected() throws Exception {
        UUID document = standard.document(DocumentStatus.PENDING);

        standard.post("/submissions", submission(List.of(document))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void infectedDocumentIsRejected() throws Exception {
        UUID document = standard.document(DocumentStatus.INFECTED);

        standard.post("/submissions", submission(List.of(document))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void anotherUsersDocumentIsRejected() throws Exception {
        UUID theirs = new PortalApi(mockMvc, keycloak, documents, "external-standard").document(DocumentStatus.CLEAN);

        standard.post("/submissions", submission(List.of(theirs))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void starterCannotSubmit() throws Exception {
        PortalApi starter = new PortalApi(mockMvc, keycloak, documents, "external-starter");

        starter.post("/submissions", submission(List.of(starter.document(DocumentStatus.CLEAN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidRequestIsRejected() throws Exception {
        standard.post("/submissions", Map.of("type", "claim with spaces", "region", "EU", "data", Map.of()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersSubmissionIs404() throws Exception {
        PortalApi other = new PortalApi(mockMvc, keycloak, documents, "external-standard");
        String theirs = PortalApi.field(
                other.post("/submissions", submission(List.of(other.document(DocumentStatus.CLEAN)))), "submissionId");

        standard.get("/submissions/" + theirs).andExpect(status().isNotFound());
        standard.post("/submissions/" + theirs + "/versions", Map.of("data", Map.of(), "documentIds", List.of()))
                .andExpect(status().isNotFound());
        standard.get("/submissions").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void correctionCreatesVersion2AndLeavesVersion1Unchanged() throws Exception {
        UUID first = standard.document(DocumentStatus.CLEAN);
        UUID second = standard.document(DocumentStatus.CLEAN);
        String id = PortalApi.field(standard.post("/submissions", submission(List.of(first))), "submissionId");

        standard.post(
                        "/submissions/" + id + "/versions",
                        Map.of("data", Map.of("amount", 150), "documentIds", List.of(first, second)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        standard.get("/submissions/" + id)
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[0].version").value(1))
                .andExpect(jsonPath("$.versions[0].data.amount").value(120))
                .andExpect(jsonPath("$.versions[0].documentIds", contains(first.toString())))
                .andExpect(jsonPath("$.versions[1].version").value(2))
                .andExpect(jsonPath("$.versions[1].data.amount").value(150))
                .andExpect(jsonPath("$.versions[1].documentIds", hasSize(2)));
    }
}
