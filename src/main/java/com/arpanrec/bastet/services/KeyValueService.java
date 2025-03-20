package com.arpanrec.bastet.services;

import com.arpanrec.bastet.encryption.Encryptor;
import com.arpanrec.bastet.exceptions.BadClient;
import com.arpanrec.bastet.exceptions.KeyValueNotFoundException;
import com.arpanrec.bastet.model.EncryptedEncryptionKey;
import com.arpanrec.bastet.model.KeyValue;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KeyValueService {
    private final KeyValueRepository keyValueRepository;

    private final EncryptedEncryptionKeyService encryptedEncryptionKeyService;

    private final Encryptor encryptor = new Encryptor();

    public KeyValueService(@Autowired KeyValueRepository keyValueRepository,
                           @Autowired EncryptedEncryptionKeyService encryptedEncryptionKeyService) {
        this.keyValueRepository = keyValueRepository;
        this.encryptedEncryptionKeyService = encryptedEncryptionKeyService;
    }

    public Integer getCurrentVersion(String key) {
        validateKey(key);
        log.trace("Getting current version for key: {}", key);
        var currentKv = keyValueRepository.findTopCurrentVersion(key);
        int currentVal = currentKv.orElse(0);
        log.trace("Current version for key: {} is: {}", key, currentVal);
        return currentVal;
    }

    public Integer getNextVersion(String key) {
        validateKey(key);
        return (getCurrentVersion(key) + 1);
    }

    public KeyValue get(String key) {
        validateKey(key);
        log.trace("Getting value for key: {}", key);
        var currentVersion = keyValueRepository.findTopCurrentVersion(key);
        int currentVal = currentVersion.orElseThrow(
            () -> new KeyValueNotFoundException("Key not found: " + key)
        );
        log.trace("Reading version: {}, key: {}", currentVal, key);
        KeyValue encryptedKV = keyValueRepository.findByKeyAndVersionAndDeletedFalse(key, currentVal);
        EncryptedEncryptionKey encryptedEncryptionKey = encryptedKV.getEncryptedEncryptionKey();
        String encryptionKey = encryptedEncryptionKeyService.unlockEncryptedEncryptionKey(encryptedEncryptionKey);
        String decryptedValue = encryptor.decrypt(encryptedKV.getValue(), encryptionKey);
        encryptedKV.setValue(decryptedValue);
        return encryptedKV;
    }

    public void save(String key, String unencryptedValue) {
        validateKey(key);
        String encryptionKey = encryptor.generateKey();
        String encryptedValue = encryptor.encrypt(unencryptedValue, encryptionKey);
        int version = getNextVersion(key);
        long currentTimestamp = System.currentTimeMillis();
        EncryptedEncryptionKey encryptedEncryptionKey = encryptedEncryptionKeyService.lockEncryptionKey(encryptionKey);
        KeyValue keyValue = new KeyValue();
        keyValue.setKey(key);
        keyValue.setValue(encryptedValue);
        log.trace("Saving version: {}, key: {}, value: {}", version, key, encryptedValue);
        keyValue.setVersion(version);
        keyValue.setEncryptedEncryptionKey(encryptedEncryptionKey);
        keyValue.setLastUpdatedAt(currentTimestamp);
        keyValueRepository.save(keyValue);
    }

    public List<String> list(String key) {
        validateKey(key);
        log.trace("Listing keys for {}", key);
        if (!key.isEmpty()) {
            key += "/";
        }
        return keyValueRepository.findAllKeysLike(key);
    }

    public void delete(String key) {
        validateKey(key);
        int currentVersion = getCurrentVersion(key);
        log.trace("Deleting version: {}, key: {}", currentVersion, key);
        keyValueRepository.setDeletedTrue(key);
    }

    private void validateKey(String key) {
        if (key == null) {
            throw new BadClient("Key cannot be null");
        }
        if (key.length() > 255) {
            throw new BadClient("Key cannot be longer than 255 characters");
        }

        if (key.startsWith("/") || key.endsWith("/")) {
            throw new BadClient("Key cannot start or end with a slash");
        }
    }
}
