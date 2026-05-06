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
    public WebPushSender webPushSender() throws Exception {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("Web Push deaktiviert: VAPID-Keys fehlen (app.vapid.public-key/private-key nicht gesetzt).");
            return new WebPushSender(null);
        }
        WebPushKeys keys = WebPushKeys.parse(publicKey.trim(), privateKey.trim(), subject.trim());
        log.info("Web Push aktiv. VAPID public-key={} (len={}), subject={}",
            maskKey(publicKey), publicKey.length(), subject);
        return new WebPushSender(keys);
    }

    public String getPublicKey() {
        return publicKey;
    }

    static String maskKey(String key) {
        if (key == null || key.length() < 24) return "<set>";
        return key.substring(0, 12) + "…" + key.substring(key.length() - 12);
    }
}
