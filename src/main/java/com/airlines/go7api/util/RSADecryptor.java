package com.airlines.go7api.util;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class RSADecryptor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RSADecryptor.class);

    private final PrivateKey privateKey;

    public RSADecryptor(String base64PrivateKey) throws Exception {
        this.privateKey = loadRSAPrivateKey(base64PrivateKey);
    }

    @SuppressWarnings("java:S5542")
    public String decrypt(String encryptedData) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"); // Secure padding scheme
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(encryptedBytes));
        } catch (Exception e) {
            logger.warn("OAEP decryption failed, trying PKCS1Padding fallback: {}", e.getMessage());
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                return new String(cipher.doFinal(encryptedBytes));
            } catch (Exception ex) {
                logger.warn("PKCS1Padding decryption failed, trying raw RSA fallback: {}", ex.getMessage());
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                return new String(cipher.doFinal(encryptedBytes));
            }
        }
    }

    private PrivateKey loadRSAPrivateKey(String privateKeyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
}
