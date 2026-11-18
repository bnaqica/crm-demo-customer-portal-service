package com.bnaqica.crmdemocustomerportalservice.support;

import com.bnaqica.crmdemocustomerportalservice.entity.Document;
import com.bnaqica.crmdemocustomerportalservice.entity.DocumentStatus;
import com.bnaqica.crmdemocustomerportalservice.repository.DocumentRepository;
import com.bnaqica.crmdemocustomerportalservice.support.IntegrationTestContainers.KeycloakContainer;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

/** A fresh portal user calling the API with a real token, plus test-only shortcuts such as already-scanned documents. */
public class PortalApi {

    public static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvc mockMvc;
    private final DocumentRepository documents;
    private final String token;
    private final UUID sub;

    public PortalApi(MockMvc mockMvc, KeycloakContainer keycloak, DocumentRepository documents, String role)
            throws Exception {
        this.mockMvc = mockMvc;
        this.documents = documents;
        var admin = new KeycloakTestAdmin(keycloak);
        String email = admin.createPortalUser(role, true);
        this.sub = UUID.fromString(admin.idOf(email));
        this.token = new TestTokens(keycloak).portalToken(email);
        get("/me"); // records the portal user
    }

    public UUID sub() {
        return sub;
    }

    public ResultActions get(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path).header("Authorization", "Bearer " + token));
    }

    public ResultActions post(String path, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body)));
    }

    /** A document of this user that has already been scanned, without going through storage and ClamAV. */
    public UUID document(DocumentStatus status) {
        UUID id = UUID.randomUUID();
        documents.save(Document.builder()
                .id(id)
                .ownerSub(sub)
                .storageKey("uploads/" + sub + "/" + id)
                .fileName("invoice.pdf")
                .contentType("application/pdf")
                .sizeBytes(1234)
                .status(status)
                .sha256(status == DocumentStatus.CLEAN ? "a".repeat(64) : null)
                .createdAt(Instant.now())
                .build());
        return id;
    }

    public static String field(ResultActions result, String name) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString())
                .get(name)
                .asString();
    }
}
