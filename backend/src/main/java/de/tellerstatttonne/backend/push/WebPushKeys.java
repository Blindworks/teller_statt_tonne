package de.tellerstatttonne.backend.push;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

/**
 * Geparste VAPID-Schlüssel + Subject. Erwartet die Formate aus
 * {@code npx web-push generate-vapid-keys}: Public Key als URL-safe Base64
 * eines unkomprimierten P-256-Points (65 Byte, beginnt mit 0x04), Private Key
 * als URL-safe Base64 eines 32-Byte rohen Skalars.
 */
public final class WebPushKeys {

    private final ECPublicKey publicKey;
    private final ECPrivateKey privateKey;
    private final byte[] uncompressedPublicKey;
    private final String publicKeyBase64Url;
    private final String subject;

    private WebPushKeys(ECPublicKey publicKey, ECPrivateKey privateKey,
                        byte[] uncompressedPublicKey, String publicKeyBase64Url,
                        String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.uncompressedPublicKey = uncompressedPublicKey;
        this.publicKeyBase64Url = publicKeyBase64Url;
        this.subject = subject;
    }

    public ECPublicKey publicKey() { return publicKey; }
    public ECPrivateKey privateKey() { return privateKey; }
    public byte[] uncompressedPublicKey() { return uncompressedPublicKey.clone(); }
    public String publicKeyBase64Url() { return publicKeyBase64Url; }
    public String subject() { return subject; }

    public static WebPushKeys parse(String publicKeyB64Url, String privateKeyB64Url, String subject)
            throws Exception {
        Base64.Decoder dec = Base64.getUrlDecoder();
        byte[] pub = dec.decode(publicKeyB64Url);
        if (pub.length != 65 || pub[0] != 0x04) {
            throw new IllegalArgumentException(
                "VAPID public key muss 65 Byte unkomprimierter P-256-Point sein (beginnt mit 0x04).");
        }
        byte[] priv = dec.decode(privateKeyB64Url);
        if (priv.length != 32) {
            throw new IllegalArgumentException(
                "VAPID private key muss ein 32-Byte roher P-256-Skalar sein.");
        }

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new java.security.spec.ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

        BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(pub, 1, 33));
        BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(pub, 33, 65));
        ECPoint w = new ECPoint(x, y);
        KeyFactory kf = KeyFactory.getInstance("EC");
        ECPublicKey pk = (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(w, ecSpec));

        BigInteger s = new BigInteger(1, priv);
        ECPrivateKey sk = (ECPrivateKey) kf.generatePrivate(new ECPrivateKeySpec(s, ecSpec));

        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(pub);
        return new WebPushKeys(pk, sk, pub, b64, subject);
    }
}
