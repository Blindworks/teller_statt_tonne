package de.tellerstatttonne.backend.push;

import java.security.Security;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebPushConfig {

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
            // Bean wird trotzdem erzeugt; Versand wirft erst beim Aufruf, damit der Start nicht scheitert,
            // wenn VAPID-Keys lokal noch nicht gesetzt sind.
            return new PushService();
        }
        PushService service = new PushService();
        service.setPublicKey(publicKey);
        service.setPrivateKey(privateKey);
        service.setSubject(subject);
        return service;
    }

    public String getPublicKey() {
        return publicKey;
    }
}
