package com.bnaqica.crmdemocustomerportalservice.client;

import com.bnaqica.crmdemocustomerportalservice.config.ClamAvProperties;
import com.bnaqica.crmdemocustomerportalservice.exception.VirusScanException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link VirusScanner} on ClamAV's {@code clamd} over TCP, with its {@code INSTREAM} command: the content goes in
 * length-prefixed chunks, and {@code clamd} answers {@code stream: OK} or {@code stream: <signature> FOUND}.
 */
@Component
@RequiredArgsConstructor
public class ClamdVirusScanner implements VirusScanner {

    private static final int CHUNK = 64 * 1024;

    private final ClamAvProperties properties;

    @Override
    public boolean isInfected(InputStream content) {
        int timeout = (int) properties.timeout().toMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(properties.host(), properties.port()), timeout);
            socket.setSoTimeout(timeout);
            var out = new DataOutputStream(socket.getOutputStream());
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            byte[] buffer = new byte[CHUNK];
            int read;
            while ((read = content.read(buffer)) > 0) {
                out.writeInt(read);
                out.write(buffer, 0, read);
            }
            out.writeInt(0);
            out.flush();
            String reply = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII)
                    .replace("\0", "")
                    .trim();
            if (reply.endsWith("OK")) {
                return false;
            }
            if (reply.endsWith("FOUND")) {
                return true;
            }
            throw new VirusScanException("Unexpected answer from clamd: " + reply, null);
        } catch (IOException ex) {
            throw new VirusScanException("Couldn't scan with clamd", ex);
        }
    }
}
