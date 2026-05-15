package com.offline.payment.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
The MeshPacket is the object that actually "hops" from phone to phone via Bluetooth.
While the letter inside is secret,
the envelope has some information written on the outside so the mesh network knows how to handle it.
 */
public class MeshPacket {
    @NotBlank
    private String packetId;

    @Min(0)
    private int ttl; //Time to Live - how many more phones this packet can jump to.

    @NotNull
    private Long createdAt; //When the sender first created the packet

    @NotBlank
    private String ciphertext; //The "Locked Letter" (The encrypted PaymentInstruction)

    public MeshPacket() {
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }
}

/*
packetId (The Tracking Number): When a stranger's phone receives a packet, it checks this ID. If it has seen this ID before,
it doesn't need to save it again. This prevents the "Gossip" from becoming an infinite loop of the same message.
 */

/*
ttl (The Life Span): This stands for Time To Live. Every time a phone passes the packet to a neighbor, it subtracts 1 from this number.
If ttl hits 0, the phone stops passing it. This is the "Postage" that ensures the packet doesn't wander the earth forever.
 */

/*
ciphertext (The Vault): This is the only part that is encrypted. It contains the PaymentInstruction we built.
The mesh nodes can see the packetId and ttl, but they cannot see inside this ciphertext.
 */
