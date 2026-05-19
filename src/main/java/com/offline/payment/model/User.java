package com.offline.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String vpa; // Primary Key: e.g., alice@upi

    // We store the key as text in the DB (length 2048 to fit standard RSA keys)
    @Column(nullable = false, length = 2048)
    private String publicKeyBase64;

    // Default constructor required by Spring Data JPA
    public User() {}

    // --- Standard Getters & Setters ---

    public String getVpa() {
        return vpa;
    }

    public void setVpa(String vpa) {
        this.vpa = vpa;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    /**
     * Helper Method: Converts the DB text back into a real Java Cryptography Key.
     * This is called by your Security services when they need to verify a signature.
     */
    public PublicKey getPublicKey() throws Exception {
        if (this.publicKeyBase64 == null || this.publicKeyBase64.isEmpty()) {
            throw new IllegalStateException("User does not have a public key registered!");
        }

        byte[] keyBytes = Base64.getDecoder().decode(this.publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}