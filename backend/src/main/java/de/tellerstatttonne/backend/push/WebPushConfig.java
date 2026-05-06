package de.tellerstatttonne.backend.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebPushConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushConfig.class);

    @Value("${app.vapid.public-key:}")
    private String publicKey;

    @Value("${app.vapid.private-key:}")
    private String privateKey;

    @Value("${app.vapid.subject:mailto:admin@local}")
    private String subject;

    @Bean
    public WebPushSender webPushSender() {
        boolean pubMissing = publicKey == null || publicKey.isBlank();
        boolean privMissing = privateKey == null || privateKey.isBlank();
        if (pubMissing || privMissing) {
            log.warn("Web Push deaktiviert: app.vapid.public-key {} / app.vapid.private-key {} (env VAPID_PUBLIC_KEY/VAPID_PRIVATE_KEY oder application.properties prüfen).",
                pubMissing ? "FEHLT" : "ok",
                privMissing ? "FEHLT" : "ok");
            return new WebPushSender(null);
        }
        try {
            WebPushKeys keys = WebPushKeys.parse(publicKey.trim(), privateKey.trim(), subject.trim());
            log.info("Web Push aktiv. VAPID public-key={} (len={}), private-key len={}, subject={}",
                maskKey(publicKey), publicKey.trim().length(), privateKey.trim().length(), subject);
            return new WebPushSender(keys);
        } catch (Exception e) {
            log.error("Web Push deaktiviert: VAPID-Keys konnten nicht geparst werden ({}). public-key-len={} private-key-len={}",
                e.getMessage(), publicKey.trim().length(), privateKey.trim().length());
            return new WebPushSender(null);
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    static String maskKey(String key) {
        if (key == null || key.length() < 24) return "<set>";
        return key.substring(0, 12) + "…" + key.substring(key.length() - 12);
    }
}
