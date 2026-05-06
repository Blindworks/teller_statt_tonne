package de.tellerstatttonne.backend.push;

import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Versendet Web-Push-Benachrichtigungen gemäß RFC 8291 (aes128gcm) +
 * RFC 8292 (VAPID v2). Bewusst ohne Drittanbieter-Library, weil
 * {@code nl.martijndwars:web-push} mit Apple Web Push zu
 * {@code BadAuthorizationHeader} führte.
 */
public class WebPushSender {

    public record Result(int status, String body) {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration JWT_TTL = Duration.ofHours(12);
    private static final byte[] KEY_INFO_PREFIX = "WebPush: info\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CEK_INFO = "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NONCE_INFO = "Content-Encoding: nonce\0".getBytes(StandardCharsets.UTF_8);

    private final WebPushKeys keys;
    private final HttpClient http;

    public WebPushSender(WebPushKeys keys) {
        this.keys = keys;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean isEnabled() {
        return keys != null;
    }

    public Result send(String endpoint, String p256dhBase64Url, String authBase64Url,
                       byte[] payload, int ttlSeconds) throws Exception {
        if (keys == null) {
            throw new IllegalStateException("Web Push ist deaktiviert (VAPID-Keys fehlen).");
        }
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getHost();

        byte[] encryptedBody = encrypt(payload, p256dhBase64Url, authBase64Url);
        String jwt = buildVapidJwt(audience);
        String authHeader = "vapid t=" + jwt + ", k=" + keys.publicKeyBase64Url();

        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", authHeader)
            .header("TTL", Integer.toString(ttlSeconds))
            .header("Content-Type", "application/octet-stream")
            .header("Content-Encoding", "aes128gcm")
            .POST(HttpRequest.BodyPublishers.ofByteArray(encryptedBody))
            .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new Result(resp.statusCode(), resp.body());
    }

    private String buildVapidJwt(String audience) {
        Instant now = Instant.now();
        return Jwts.builder()
            .header().add("typ", "JWT").and()
            .audience().add(audience).and()
            .subject(keys.subject())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(JWT_TTL)))
            .signWith(keys.privateKey(), Jwts.SIG.ES256)
            .compact();
    }

    private byte[] encrypt(byte[] plaintext, String uaPubB64, String authB64) throws Exception {
        Base64.Decoder dec = Base64.getUrlDecoder();
        byte[] uaPub = dec.decode(uaPubB64);
        if (uaPub.length != 65 || uaPub[0] != 0x04) {
            throw new IllegalArgumentException("Subscription p256dh hat falsches Format.");
        }
        byte[] authSecret = dec.decode(authB64);
        ECPublicKey uaPubKey = parseUncompressedPublicKey(uaPub);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair as = kpg.genKeyPair();
        ECPublicKey asPub = (ECPublicKey) as.getPublic();
        byte[] asPubUncompressed = uncompressedPoint(asPub);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(as.getPrivate());
        ka.doPhase(uaPubKey, true);
        byte[] ecdhSecret = ka.generateSecret();

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        byte[] prkKey = hmac(authSecret, ecdhSecret);

        byte[] keyInfo = concat(KEY_INFO_PREFIX, uaPub, asPubUncompressed);
        byte[] ikm = hmac(prkKey, concat(keyInfo, new byte[]{0x01}));

        byte[] prk = hmac(salt, ikm);

        byte[] cek = trim(hmac(prk, concat(CEK_INFO, new byte[]{0x01})), 16);
        byte[] nonce = trim(hmac(prk, concat(NONCE_INFO, new byte[]{0x01})), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"),
            new GCMParameterSpec(128, nonce));
        byte[] padded = concat(plaintext, new byte[]{0x02});
        byte[] cipherText = cipher.doFinal(padded);

        ByteBuffer recordSize = ByteBuffer.allocate(4).putInt(4096);
        byte[] header = concat(salt, recordSize.array(),
            new byte[]{(byte) asPubUncompressed.length}, asPubUncompressed);
        return concat(header, cipherText);
    }

    private static ECPublicKey parseUncompressedPublicKey(byte[] uncompressed) throws Exception {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 33, 65));
        ECPoint w = new ECPoint(x, y);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, ecSpec));
    }

    private static byte[] uncompressedPoint(ECPublicKey key) {
        ECPoint w = key.getW();
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(unsigned32(w.getAffineX()), 0, out, 1, 32);
        System.arraycopy(unsigned32(w.getAffineY()), 0, out, 33, 32);
        return out;
    }

    private static byte[] unsigned32(BigInteger v) {
        byte[] b = v.toByteArray();
        if (b.length == 32) return b;
        byte[] out = new byte[32];
        if (b.length > 32) {
            System.arraycopy(b, b.length - 32, out, 0, 32);
        } else {
            System.arraycopy(b, 0, out, 32 - b.length, b.length);
        }
        return out;
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        byte[] out = new byte[len];
        int off = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, off, p.length);
            off += p.length;
        }
        return out;
    }

    private static byte[] trim(byte[] arr, int len) {
        byte[] out = new byte[len];
        System.arraycopy(arr, 0, out, 0, len);
        return out;
    }
}
