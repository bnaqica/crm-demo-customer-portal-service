package com.bnaqica.crmdemocustomerportalservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.KeycloakTestAdmin;
import com.bnaqica.crmdemocustomerportalservice.support.TestDocuments;
import com.bnaqica.crmdemocustomerportalservice.support.TestStorage;
import com.bnaqica.crmdemocustomerportalservice.support.TestTokens;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.localstack.LocalStackContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** document-uploads spec, "Virus scan", end to end: pre-signed upload, S3 notification over SQS, ClamAV. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class DocumentScanTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    @Autowired
    private LocalStackContainer localStack;

    private String token;

    @BeforeEach
    void setUp() {
        token = new TestTokens(keycloak)
                .portalToken(new KeycloakTestAdmin(keycloak).createPortalUser("external-starter", true));
    }

    private JsonNode upload(byte[] content) throws Exception {
        String body = mockMvc.perform(post("/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "fileName", "file.pdf", "contentType", "application/pdf", "size", content.length))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode upload = JSON.readTree(body);
        Map<String, String> headers = JSON.convertValue(upload.get("uploadHeaders"), Map.class);
        assertThat(TestStorage.put(upload.get("uploadUrl").asString(), headers, content))
                .isEqualTo(200);
        return upload;
    }

    private JsonNode awaitScanned(String documentId) {
        return await().atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(1))
                .until(
                        () -> JSON.readTree(mockMvc.perform(
                                        get("/documents/" + documentId).header("Authorization", "Bearer " + token))
                                .andReturn()
                                .getResponse()
                                .getContentAsString()),
                        document -> !"PENDING".equals(document.get("status").asString()));
    }

    @Test
    void infectedUploadIsDeletedAndMarkedInfected() throws Exception {
        String documentId = upload(TestDocuments.eicar()).get("documentId").asString();

        JsonNode document = awaitScanned(documentId);

        assertThat(document.get("status").asString()).isEqualTo("INFECTED");
        assertThat(TestStorage.exists(localStack, TestDocuments.key(token, documentId)))
                .isFalse();
    }

    /** The upload URL works until it expires, but only for the first upload, so a scanned file can't be swapped. */
    @Test
    void uploadUrlCannotReplaceAScannedFile() throws Exception {
        byte[] clean = "%PDF-1.7 the file that gets scanned".getBytes(StandardCharsets.UTF_8);
        JsonNode upload = upload(clean);
        String documentId = upload.get("documentId").asString();
        assertThat(awaitScanned(documentId).get("status").asString()).isEqualTo("CLEAN");

        byte[] swapped = new byte[clean.length];
        System.arraycopy(TestDocuments.eicar(), 0, swapped, 0, Math.min(swapped.length, TestDocuments.eicar().length));
        Map<String, String> headers = JSON.convertValue(upload.get("uploadHeaders"), Map.class);
        int status = TestStorage.put(upload.get("uploadUrl").asString(), headers, swapped);

        assertThat(status).isEqualTo(412);
    }

    @Test
    void cleanUploadIsMarkedClean() throws Exception {
        String documentId = upload("%PDF-1.7 a clean test file".getBytes(StandardCharsets.UTF_8))
                .get("documentId")
                .asString();

        assertThat(awaitScanned(documentId).get("status").asString()).isEqualTo("CLEAN");
        assertThat(TestStorage.exists(localStack, TestDocuments.key(token, documentId)))
                .isTrue();
    }
}
