package com.arpanrec.bastet.physical;

public record EncryptedValue(
    String encryptedValue,
    String encryptionKeyHash
) {
}
