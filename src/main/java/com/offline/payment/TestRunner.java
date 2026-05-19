package com.offline.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offline.payment.model.MeshPacket;
import com.offline.payment.model.PaymentInstruction;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class TestRunner {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_BITS = 256;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    public static void main(String[] args) {
        try {
            System.out.println("🚀 Starting Headless Client Offline UPI Simulation...");
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            // 1. ALICE GENERATES KEYS ON HER PHONE
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair aliceKeyPair = keyGen.generateKeyPair();
            String alicePubKeyBase64 = Base64.getEncoder().encodeToString(aliceKeyPair.getPublic().getEncoded());

            // 2. WI-FI ONBOARDING: Register with Bank & Get Bank's Key
            System.out.println("🌐 [ONLINE] Provisioning Alice's device with the Bank...");
            Map<String, String> provisionReq = Map.of("vpa", "alice@upi", "publicKey", alicePubKeyBase64);

            HttpRequest setupRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/mesh/provision"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(provisionReq)))
                    .build();

            HttpResponse<String> setupResponse = client.send(setupRequest, HttpResponse.BodyHandlers.ofString());

            // --- NEW SAFETY CHECK ADDED HERE ---
            if (setupResponse.statusCode() != 200) {
                System.err.println("❌ Provisioning Failed! The Bank Server rejected the setup.");
                System.err.println("Server HTTP Code: " + setupResponse.statusCode());
                System.err.println("Server Error Response: " + setupResponse.body());
                System.err.println("👉 Check your Spring Boot console for the SQL or logic error.");
                return; // Stop the script gracefully
            }
            // -----------------------------------

            Map<?, ?> setupData = mapper.readValue(setupResponse.body(), Map.class);
            String bankPubKeyBase64 = (String) setupData.get("bankPublicKey");

            // Reconstruct Bank's Public Key from the Base64 string
            byte[] bankKeyBytes = Base64.getDecoder().decode(bankPubKeyBase64);
            PublicKey bankPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bankKeyBytes));
            System.out.println("✅ Provisioning Complete. Acquired Bank Public Key.");

            // ---------------------------------------------------------
            // 3. PHASE 1: ALICE WRITES AN OFFLINE TRANSACTION
            System.out.println("\n📱 [OFFLINE] Alice initiating offline payment of ₹500.00 to Bob...");
            PaymentInstruction instruction = new PaymentInstruction(
                    "alice@upi", "bob@upi", new BigDecimal("500.00"),
                    "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
                    UUID.randomUUID().toString(), Instant.now().toEpochMilli()
            );

            byte[] plaintextBytes = mapper.writeValueAsBytes(instruction);

            // 4. HYBRID ENCRYPTION (Using the REAL Bank Public Key)
            KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");
            aesKeyGen.init(AES_KEY_BITS);
            SecretKey temporaryAesKey = aesKeyGen.generateKey();

            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
            aesCipher.init(Cipher.ENCRYPT_MODE, temporaryAesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encryptedPayload = aesCipher.doFinal(plaintextBytes);

            Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
            rsaCipher.init(Cipher.ENCRYPT_MODE, bankPublicKey); // Removed the explicit OAEP spec to match backend!
            byte[] encryptedAesKey = rsaCipher.doFinal(temporaryAesKey.getEncoded());

            // Pack into a colon-separated string matching the backend's exact format: IV:Key:Payload
            String ciphertextBase64 = Base64.getEncoder().encodeToString(iv) + ":" +
                    Base64.getEncoder().encodeToString(encryptedAesKey) + ":" +
                    Base64.getEncoder().encodeToString(encryptedPayload);

            System.out.println("🔒 Payload encrypted into Hybrid Ciphertext Safe (Formatted perfectly!).");

            // 5. DIGITAL SIGNATURE
            Signature signatureEngine = Signature.getInstance("SHA256withRSA");
            signatureEngine.initSign(aliceKeyPair.getPrivate());
            signatureEngine.update(ciphertextBase64.getBytes());
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureEngine.sign());
            System.out.println("🔏 Wax seal signature applied.");

            // 6. PACK THE ENVELOPE
            MeshPacket packet = new MeshPacket();
            packet.setPacketId(UUID.randomUUID().toString());
            packet.setTtl(5);
            packet.setCreatedAt(Instant.now().toEpochMilli());
            packet.setCiphertext(ciphertextBase64);
            packet.setSenderVpa("alice@upi");
            packet.setSignature(signatureBase64);

            // 7. DAVE TRANSMITS VIA 5G
            System.out.println("\n📡 [ONLINE] Dave found network! Uploading transaction packet...");
            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/mesh/upload"))
                    .header("Content-Type", "application/json")
                    .header("X-Bridge-Node-Id", "phone-dave-gateway")
                    .header("X-Hop-Count", "2")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(packet)))
                    .build();

            HttpResponse<String> uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("\n📥 Server Response Received! Code: " + uploadResponse.statusCode());
            System.out.println("💬 Response Payload: " + uploadResponse.body());
            System.out.println("\n🏁 Simulation completed successfully. Payment Successfully Completed!..");

        } catch (Exception e) {
            System.err.println("❌ Critical Simulation Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}