package com.arpanrec.bastet.services;

import com.arpanrec.bastet.encryption.Encryptor;
import com.arpanrec.bastet.exceptions.CaughtException;
import com.arpanrec.bastet.hash.Hashing;
import com.arpanrec.bastet.model.EncryptedEncryptionKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptedEncryptionKeyService {

    private final EncryptedEncryptionKeyRepository encryptedEncryptionKeyRepository;

    @Getter
    private String unlockKey;

    private final Encryptor encryptor = new Encryptor();
    private final Hashing hashing = new Hashing();

    public EncryptedEncryptionKeyService(@Autowired EncryptedEncryptionKeyRepository encryptedEncryptionKeyRepository,
                                         @Value("${bastet.unlock-key:#{null}}") String unlockKey) {
        this.encryptedEncryptionKeyRepository = encryptedEncryptionKeyRepository;
        this.unlockKey = unlockKey;
    }

    public void saveEncryptionKey(EncryptedEncryptionKey encryptedEncryptionKey) {
        encryptedEncryptionKeyRepository.save(encryptedEncryptionKey);
    }

    public void setUnlockKey(String unlockKey) {
        if (this.unlockKey != null) {
            throw new CaughtException("Master key already set");
        } else {
            this.unlockKey = unlockKey;
        }
    }

    public String unlockEncryptedEncryptionKey(EncryptedEncryptionKey encryptedEncryptionKey) {
        if (this.unlockKey == null) {
            throw new CaughtException("Master key not set");
        }
        if (!hashing.matches(unlockKey, encryptedEncryptionKey.getEncryptorKeyHash())) {
            throw new CaughtException("Encryption key was locked with a different master key");
        }
        return encryptor.decrypt(encryptedEncryptionKey.getEncryptedEncryptionKey(), unlockKey);
    }

    public void saveEncryptedEncryptionKey(String key) {
        EncryptedEncryptionKey encryptedEncryptionKey = this.lockEncryptionKey(key);
        this.saveEncryptionKey(encryptedEncryptionKey);
    }

    public EncryptedEncryptionKey lockEncryptionKey(String encryptionKey) {
        if (this.unlockKey == null) {
            throw new CaughtException("Master key not set");
        }
        EncryptedEncryptionKey ek = new EncryptedEncryptionKey();
        ek.setEncryptedEncryptionKey(encryptor.encrypt(encryptionKey, unlockKey));
        ek.setEncryptorKeyHash(hashing.encode(unlockKey));
        ek.setEncryptionKeyHash(hashing.encode(encryptionKey));
        return ek;
    }
}
