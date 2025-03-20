package com.arpanrec.bastet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity(name = "encrypted_encryption_keys_t")
@AllArgsConstructor
@NoArgsConstructor
public class EncryptedEncryptionKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_c")
    private Long id;

    @Column(name = "encrypted_encryption_key_c", nullable = false, columnDefinition = "TEXT")
    private String encryptedEncryptionKey;

    @Column(name = "encryption_key_hash_c", nullable = false, unique = true)
    private String encryptionKeyHash;

    @Column(name = "encryptor_key_hash_c", nullable = false)
    private String encryptorKeyHash;

}
