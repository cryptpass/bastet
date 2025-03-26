package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.ConfigService;
import com.arpanrec.bastet.encryption.Encryptor;
import com.arpanrec.bastet.exceptions.BadClient;
import com.arpanrec.bastet.exceptions.KeyValueNotFoundException;
import com.arpanrec.bastet.exceptions.PhysicalException;
import com.arpanrec.bastet.hash.Hashing;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Physical implements UserDetailsService {

    private final IPhysical physical;

    private String masterKey;

    private String masterKeyHash;

    private final Encryptor encryptor = Encryptor.INSTANCE;

    private final Hashing hashing = Hashing.INSTANCE;

    public Physical(@Autowired ConfigService configService) {
        Config physicalConfig = configService.getConfig().physical();
        if (physicalConfig.getMasterKey() != null) {
            setMasterKey(physicalConfig.getMasterKey());
            log.debug("Master key set from config");
        }

        switch (physicalConfig.getType()) {
            case SQLITE:
                physical = new Sqlite3(physicalConfig.getConfig());
                break;
            case LIBSQL:
                physical = new LibSQL(physicalConfig.getConfig());
                break;
            case POSTGRES:
                physical = new Postgres(physicalConfig.getConfig());
                break;
            default:
                throw new PhysicalException("Unknown physical type: " + physicalConfig.getType());
        }
    }

    public String read(String key) {
        log.trace("Reading key: {}", key);
        validateKey(key);
        int currentVersion = physical.getCurrentVersion(key);
        if (currentVersion == 0) {
            throw new KeyValueNotFoundException("No version found for key: " + key);
        }
        Physical.EncryptedValue encryptedValue = physical.read(key, currentVersion);
        if (encryptedValue == null) {
            throw new KeyValueNotFoundException("Key not found, Key: " + key + " Version: " + currentVersion);
        }
        EncryptedEncryptionKey encryptedEncryptionKey = physical.readEncryptedKey(encryptedValue.encryptionKeyHash());
        if (encryptedEncryptionKey == null) {
            throw new KeyValueNotFoundException("Missing data encryption key, Key: " + key + ", Version: " + currentVersion + ", Encryption key hash: " + encryptedValue.encryptionKeyHash());
        }
        if (!hashing.matches(masterKey, encryptedEncryptionKey.encryptorKeyHash())) {
            throw new KeyValueNotFoundException("Master key mismatch, Data encrypted with key with hash: " + encryptedEncryptionKey.encryptorKeyHash() + ", Expected :" + hashing.encode(masterKey) + ", Key: " + key + ", Version: " + currentVersion);
        }
        return encryptor.decrypt(encryptedValue.encryptedValue(),
            encryptor.decrypt(encryptedEncryptionKey.encryptedEncryptionKey(), masterKey));
    }

    public void write(String key, String value) {
        log.trace("Writing key: {}", key);
        validateKey(key);
        int nextVersion = physical.getNextVersion(key);
        String encryptionKey = encryptor.generateKey();
        EncryptedEncryptionKey encryptedEncryptionKey = new EncryptedEncryptionKey(encryptor.encrypt(encryptionKey,
            masterKey), hashing.encode(encryptionKey), masterKeyHash);
        log.debug("Encryption key created for key: {}, version: {}, encryption key hash: {}", key, nextVersion,
            encryptedEncryptionKey.encryptionKeyHash());
        physical.writeEncryptedKey(encryptedEncryptionKey);
        String encryptedValue = encryptor.encrypt(value, encryptionKey);
        physical.write(key, encryptedValue, nextVersion, encryptedEncryptionKey.encryptionKeyHash());
    }

    public void delete(String key, int version) {
        log.trace("Deleting key: {}", key);
        validateKey(key);
        physical.delete(key, version);
    }

    public void deleteAll(String key) {
        log.trace("Deleting all versions for key: {}", key);
        validateKey(key);
        log.trace("Deleting all keys for {}", key);
        int currentVersion = physical.getCurrentVersion(key);
        while (currentVersion > 0) {
            int deleteVersion = physical.getCurrentVersion(key);
            log.trace("Deleting version {} for key {}", deleteVersion, key);
            delete(key, deleteVersion);
            currentVersion = physical.getCurrentVersion(key);
        }
    }

    public List<String> listKeys(String key) {
        validateKey(key, true);
        if (!key.isEmpty()) {
            return physical.listKeys(key + "/");
        } else {
            return physical.listKeys(key);
        }
    }

    private void validateKey(String key) {
        validateKey(key, false);
    }

    private void validateKey(String key, boolean isListing) {
        if (key == null) {
            throw new BadClient("Key cannot be null");
        }
        if (!isListing && key.isEmpty()) {
            throw new BadClient("Key cannot be empty when not listing");
        }
        if (key.startsWith("/") || key.endsWith("/")) {
            throw new BadClient("Key cannot start or end with /");
        }
        if (masterKey == null) {
            throw new BadClient("Master key is not set");
        }
    }

    public void setMasterKey(String masterKey) {
        if (masterKey == null || masterKey.isEmpty()) {
            throw new BadClient("Master key cannot be null");
        }

        if ("random".equals(masterKey)) {
            this.masterKey = encryptor.generateKey();
            log.info("Generated random master key");
        } else {
            this.masterKey = masterKey;
            log.info("Master key set");
        }
        this.masterKeyHash = hashing.encode(this.masterKey);
    }

    public Optional<User> readUser(String username) {
        return physical.readUser(username);
    }

    public void writeUser(User user) {
        Optional<User> userOptional = readUser(user.getUsername());
        if (userOptional.isPresent()) {
            physical.updateUser(user);
        } else {
            physical.createUser(user);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void writeUser(Map<String, Object> userDetails) {
        Optional<User> userOptional = readUser(userDetails.get("username").toString());
        User user = userOptional.orElseGet(User::new);

        user.setUsername(userDetails.get("username").toString());

        if (userDetails.containsKey("password") && userDetails.get("password") != null
            && userDetails.containsKey("passwordHash") && userDetails.get("passwordHash") != null) {
            throw new BadClient("Cannot set both password and passwordHash");
        }

        if (userDetails.containsKey("email") && userDetails.get("email") != null) {
            log.debug("Updating email for user {}, email: {}", userDetails.get("username"), userDetails.get("email"));
            user.setEmail(userDetails.get("email").toString());
        }

        if (userDetails.containsKey("password") && userDetails.get("password") != null) {
            log.debug("Updating password for user {}", userDetails.get("username"));
            user.setPassword(userDetails.get("password").toString());
        }

        if (userDetails.containsKey("passwordHash") && userDetails.get("passwordHash") != null) {
            log.debug("Updating passwordHash for user {}", userDetails.get("username"));
            user.setPasswordHash(userDetails.get("passwordHash").toString());
        }

        if (userDetails.containsKey("roles") && userDetails.get("roles") != null) {
            try {
                user.setRoles((List<User.Role>) userDetails.get("roles"));
                log.debug("Updating roles for user {}, roles: {}", userDetails.get("username"), userDetails.get(
                    "roles"));
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to convert JSON string to list of roles", e);
            }
        }

        if (userDetails.containsKey("locked") && userDetails.get("locked") != null) {
            try {
                log.debug("Updating locked for user {}, locked: {}", userDetails.get("username"), userDetails.get(
                    "locked"));
                user.setLocked((boolean) userDetails.get("locked"));
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to convert JSON string to boolean", e);
            }
        }

        if (userDetails.containsKey("enabled") && userDetails.get("enabled") != null) {
            try {
                log.debug("Updating enabled for user {}, enabled: {}", userDetails.get("username"), userDetails.get(
                    "enabled"));
                user.setEnabled((boolean) userDetails.get("enabled"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert JSON string to boolean", e);
            }
        }

        if (userOptional.isEmpty()) {
            physical.createUser(user);
        } else {
            physical.updateUser(user);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return readUser(username).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Data
    public static class Config {
        Type type;
        @JsonProperty("master-key")
        String masterKey;
        Map<String, Object> config;

        public enum Type {
            SQLITE, LIBSQL, POSTGRES
        }
    }

    public record EncryptedEncryptionKey(
        String encryptedEncryptionKey,
        String encryptionKeyHash,
        String encryptorKeyHash) {
    }

    public record EncryptedValue(
        String encryptedValue,
        String encryptionKeyHash) {
    }
}
