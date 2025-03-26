package com.arpanrec.bastet.physical;

import java.util.List;
import java.util.Optional;

public interface IPhysical {

    int getCurrentVersion(String key);

    int getNextVersion(String key);

    Physical.EncryptedValue read(String key, int version);

    void write(String key, String value, int version, String encryptorKeyHash);

    void delete(String key, int version);

    List<String> listKeys(String key);

    Physical.EncryptedEncryptionKey readEncryptedKey(String encryption_key_hash_c);

    void writeEncryptedKey(Physical.EncryptedEncryptionKey encryptedEncryptionKey);

    Optional<User> readUser(String username);

    void createUser(User user);

    void updateUser(User user);
}
