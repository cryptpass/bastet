package com.arpanrec.bastet.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity(name = "key_value_t")
@Table(uniqueConstraints =
    {@UniqueConstraint(columnNames = {"key_c", "version_c"})}
)
@AllArgsConstructor
@NoArgsConstructor
public class KeyValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_c")
    private Long id;

    @Column(name = "key_c", nullable = false)
    private String key;

    @Column(name = "value_c", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "deleted_c", nullable = false)
    private boolean deleted = false;

    @Column(name = "version_c", nullable = false)
    private Integer version;

    @ManyToOne(cascade = CascadeType.ALL, targetEntity = EncryptedEncryptionKey.class)
    @JoinColumn(name = "encryptor_key_hash_c", referencedColumnName = "encryption_key_hash_c")
    private EncryptedEncryptionKey encryptedEncryptionKey;

    @Column(name = "last_updated_at_c", nullable = false)
    private Long lastUpdatedAt;
}
