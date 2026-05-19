package com.offline.payment.security;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class HybridCryptoService {

    // Inject our centralized Bank Master Key Holder!
    private final ServerKeyHolder serverKeyHolder;

    public HybridCryptoService(ServerKeyHolder serverKeyHolder) {
        this.serverKeyHolder = serverKeyHolder;
    }

    public String encryptPayload(String plainTextJson) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] encryptedData = aesCipher.doFinal(plainTextJson.getBytes());

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        // USE THE CENTRALIZED PUBLIC KEY
        rsaCipher.init(Cipher.ENCRYPT_MODE, serverKeyHolder.getPublicKey());
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        return Base64.getEncoder().encodeToString(iv) + ":" +
                Base64.getEncoder().encodeToString(encryptedAesKey) + ":" +
                Base64.getEncoder().encodeToString(encryptedData);
    }

    public String decryptPayload(String ciphertext) throws Exception {
        String[] parts = ciphertext.split(":");
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encryptedAesKey = Base64.getDecoder().decode(parts[1]);
        byte[] encryptedData = Base64.getDecoder().decode(parts[2]);

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        // USE THE CENTRALIZED PRIVATE KEY
        rsaCipher.init(Cipher.DECRYPT_MODE, serverKeyHolder.getPrivateKey());
        byte[] decryptedAesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
        SecretKey originalAesKey = new javax.crypto.spec.SecretKeySpec(decryptedAesKeyBytes, "AES");

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, originalAesKey, spec);
        byte[] plainTextBytes = aesCipher.doFinal(encryptedData);

        return new String(plainTextBytes);
    }
}