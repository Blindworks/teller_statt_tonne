package de.tellerstatttonne.backend.push;

import java.security.Security;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebPushConfig {

    private static final Logger log = LoggerFactory.getLogger(WebPushConfig.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${app.vapid.public-key:}")
    private String publicKey;

    @Value("${app.vapid.private-key:}")
    private String privateKey;

    @Value("${app.vapid.subject:mailto:admin@local}")
    private String subject;

    @Bean
    public PushService pushService() throws Exception {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("Web Push deaktiviert: VAPID-Keys fehlen (app.vapid.public-key/private-key nicht gesetzt).");
            return new PushService();
        }
        log.info("Web Push aktiv. VAPID public-key={} (len={}), subject={}",
            maskKey(publicKey), publicKey.length(), subject);
        PushService service = new PushService();
        service.setPublicKey(publicKey);
        service.setPrivateKey(privateKey);
        service.setSubject(subject);
        return service;
    }

    public String getPublicKey() {
        return publicKey;
    }

    static String maskKey(String key) {
        if (key == null || key.length() < 24) return "<set>";
        return key.substring(0, 12) + "…" + key.substring(key.length() - 12);
    }
}
