package com.offline.payment.model;

import java.math.BigDecimal;

public class PaymentInstruction {

    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;
    private String pinHash;
    private String nonce; // UUID, unique per payment intent
    private Long signedAt;// epoch millis, when sender signed

    public PaymentInstruction() {
    }

    public PaymentInstruction(String senderVpa, String receiverVpa, BigDecimal amount, String pinHash, String nonce, Long signedAt) {
        this.senderVpa = senderVpa;
        this.receiverVpa = receiverVpa;
        this.amount = amount;
        this.pinHash = pinHash;
        this.nonce = nonce;
        this.signedAt = signedAt;
    }

    public String getSenderVpa() {
        return senderVpa;
    }

    public void setSenderVpa(String senderVpa) {
        this.senderVpa = senderVpa;
    }

    public String getReceiverVpa() {
        return receiverVpa;
    }

    public void setReceiverVpa(String receiverVpa) {
        this.receiverVpa = receiverVpa;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Long getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Long signedAt) {
        this.signedAt = signedAt;
    }
}

/*
1-> The Nonce (Number Used once)
This is a random UUID.

The Problem: If Alice pays Bob ₹100 every morning,
the encrypted packet would look exactly the same every day.
A hacker could notice this pattern.

The Fix: By adding a random nonce,
every single packet Alice sends looks completely different to the outside world,
even if the amount and receiver are identical.
 */


/*
2-> The pinHash
We never send the actual UPI PIN (like 1-2-3-4) through the mesh.
The Logic: The phone hashes the PIN locally before putting it in this class.
If a stranger somehow broke the encryption (which is nearly impossible),
they’d only find a scrambled hash, not your actual PIN.
 */

/*
3-> signedAt Timestamp
The server used this to ensure the packet isn't a "Zombie Packet" from weeks ago.
If the signedAt time is older than the server's allowed window(eg 24 hr), it gets rejected instantly.
 */