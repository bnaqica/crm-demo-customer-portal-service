package com.bnaqica.crmdemocustomerportalservice.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import tools.jackson.databind.json.JsonMapper;

/** Test files. */
public final class TestDocuments {

    private TestDocuments() {}

    /**
     * The EICAR anti-virus test file, which every scanner reports as infected but which is harmless. Built at run
     * time so this source file itself isn't flagged by a scanner on the developer's machine.
     */
    public static byte[] eicar() {
        String reversed = "*H+H$!ELIF-TSET-SURIVITNA-DRADNATS-RACIE$}7)CC7)^P(45XZP\\4[PA@%P!O5X";
        return new StringBuilder(reversed).reverse().toString().getBytes(StandardCharsets.US_ASCII);
    }

    /** The storage key of a document, {@code uploads/{sub}/{documentId}}, from the uploader's token. */
    public static String key(String token, String documentId) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        String sub = JsonMapper.builder().build().readTree(payload).get("sub").asString();
        return "uploads/" + sub + "/" + documentId;
    }
}
