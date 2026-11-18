package com.bnaqica.crmdemocustomerportalservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnaqica.crmdemocustomerportalservice.config.ClamAvProperties;
import com.bnaqica.crmdemocustomerportalservice.exception.VirusScanException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** The clamd protocol against a fake clamd that answers one connection. The real one is in DocumentScanTests. */
class ClamdVirusScannerTests {

    private static boolean scanWithReply(String reply) throws Exception {
        try (var server = new ServerSocket(0)) {
            var fake = CompletableFuture.runAsync(() -> {
                try (var socket = server.accept()) {
                    var in = new DataInputStream(socket.getInputStream());
                    in.readNBytes("zINSTREAM\0".length());
                    int length;
                    while ((length = in.readInt()) > 0) {
                        in.readNBytes(length);
                    }
                    socket.getOutputStream().write((reply + "\0").getBytes(StandardCharsets.US_ASCII));
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            });
            var scanner = new ClamdVirusScanner(
                    new ClamAvProperties("localhost", server.getLocalPort(), Duration.ofSeconds(5)));
            try {
                return scanner.isInfected(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));
            } finally {
                fake.join();
            }
        }
    }

    @Test
    void okReplyIsClean() throws Exception {
        assertThat(scanWithReply("stream: OK")).isFalse();
    }

    @Test
    void foundReplyIsInfected() throws Exception {
        assertThat(scanWithReply("stream: Win.Test.EICAR_HDB-1 FOUND")).isTrue();
    }

    @Test
    void errorReplyFailsTheScan() {
        assertThatThrownBy(() -> scanWithReply("INSTREAM size limit exceeded. ERROR"))
                .isInstanceOf(VirusScanException.class);
    }
}
