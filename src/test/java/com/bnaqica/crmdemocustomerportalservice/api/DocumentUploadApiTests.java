package com.bnaqica.crmdemocustomerportalservice.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import com.bnaqica.crmdemocustomerportalservice.support.KeycloakTestAdmin;
import com.bnaqica.crmdemocustomerportalservice.support.TestStorage;
import com.bnaqica.crmdemocustomerportalservice.support.TestTokens;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** document-uploads spec, the request scenarios, with real tokens and LocalStack S3. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestContainers.class)
class DocumentUploadApiTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final long MB = 1024 * 1024;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakContainer keycloak;

    private KeycloakTestAdmin admin;
    private TestTokens tokens;

    @BeforeEach
    void setUp() {
        admin = new KeycloakTestAdmin(keycloak);
        tokens = new TestTokens(keycloak);
    }

    private ResultActions requestUpload(String token, String contentType, long size) throws Exception {
        return mockMvc.perform(post("/documents")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(
                        Map.of("fileName", "invoice.pdf", "contentType", contentType, "size", size))));
    }

    @Test
    void allowedFileGetsAShortLivedUploadUrlThatWorks() throws Exception {
        String token = tokens.portalToken(admin.createPortalUser("external-standard", true));

        String body = requestUpload(token, "application/pdf", 2 * MB)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode upload = JSON.readTree(body);
        URI url = URI.create(upload.get("uploadUrl").asString());
        var query = UriComponentsBuilder.fromUri(url).build().getQueryParams();
        assertThat(Integer.parseInt(query.getFirst("X-Amz-Expires"))).isLessThanOrEqualTo(600);
        Map<String, String> headers = JSON.convertValue(upload.get("uploadHeaders"), Map.class);
        assertThat(TestStorage.put(url.toString(), headers, new byte[(int) (2 * MB)]))
                .isEqualTo(200);
    }

    @Test
    void disallowedTypeIsRejected() throws Exception {
        String token = tokens.portalToken(admin.createPortalUser("external-standard", true));

        requestUpload(token, "application/x-msdownload", MB).andExpect(status().isUnprocessableContent());
    }

    @Test
    void fileAboveTheSizeLimitIsRejected() throws Exception {
        String token = tokens.portalToken(admin.createPortalUser("external-standard", true));

        requestUpload(token, "application/pdf", 11 * MB).andExpect(status().isUnprocessableContent());
    }

    /** Starter quota is 20 MB in the defaults; pending uploads count towards it. */
    @Test
    void starterQuotaExceededIsRejected() throws Exception {
        String token = tokens.portalToken(admin.createPortalUser("external-starter", true));
        requestUpload(token, "application/pdf", 9 * MB).andExpect(status().isCreated());
        requestUpload(token, "application/pdf", 9 * MB).andExpect(status().isCreated());

        requestUpload(token, "application/pdf", 9 * MB).andExpect(status().isUnprocessableContent());
    }

    @Test
    void standardQuotaIsHigherThanStarters() throws Exception {
        String token = tokens.portalToken(admin.createPortalUser("external-standard", true));
        for (int i = 0; i < 3; i++) {
            requestUpload(token, "application/pdf", 9 * MB).andExpect(status().isCreated());
        }
    }

    @Test
    void ownerSeesTheDocumentAndOthersGet404() throws Exception {
        String owner = tokens.portalToken(admin.createPortalUser("external-starter", true));
        String other = tokens.portalToken(admin.createPortalUser("external-starter", true));
        String documentId = JSON.readTree(requestUpload(owner, "application/pdf", MB)
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("documentId")
                .asString();

        mockMvc.perform(get("/documents/" + documentId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fileName").value("invoice.pdf"));
        mockMvc.perform(get("/documents/" + documentId).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }
}
