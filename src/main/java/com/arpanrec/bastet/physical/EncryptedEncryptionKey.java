package com.arpanrec.bastet.physical;

public record EncryptedEncryptionKey(
    String encryptedEncryptionKey,
    String encryptionKeyHash,
    String encryptorKeyHash) {
}
