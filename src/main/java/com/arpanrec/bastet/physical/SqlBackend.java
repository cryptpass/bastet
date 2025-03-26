package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.exceptions.PhysicalException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class SqlBackend implements IPhysical {

    protected void createTable() {
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS encryption_keys_t ("
                + "id_c INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "encrypted_encryption_key_c TEXT NOT NULL, "
                + "encryption_key_hash_c TEXT NOT NULL, "
                + "encryptor_key_hash_c TEXT NOT NULL, "
                + "UNIQUE (encryption_key_hash_c));");
            statement.execute("CREATE TABLE IF NOT EXISTS key_value_t ("
                + "id_c INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "key_c TEXT NOT NULL, "
                + "value_c TEXT NOT NULL, "
                + "deleted_c BOOLEAN DEFAULT FALSE NOT NULL, "
                + "version_c INTEGER NOT NULL, "
                + "updated_at_c INTEGER NOT NULL, "
                + "encryptor_key_hash_c TEXT NOT NULL, "
                + "FOREIGN KEY (encryptor_key_hash_c) REFERENCES encryption_keys_t (encryption_key_hash_c), "
                + "UNIQUE (key_c, version_c));");
            statement.execute("CREATE TABLE IF NOT EXISTS users_t ("
                + "id_c INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username_c TEXT NOT NULL, "
                + "email_c TEXT, "
                + "password_hash_c TEXT, "
                + "password_last_changed_c INTEGER, "
                + "roles_c TEXT NOT NULL, "
                + "lastLogin_c INTEGER, "
                + "locked_c BOOLEAN DEFAULT TRUE NOT NULL, "
                + "enabled_c BOOLEAN DEFAULT FALSE NOT NULL, "
                + "UNIQUE (username_c));");
            log.info("Created table users_t");
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    protected abstract Connection getConnect();

    protected void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to close connection", e);
        }
    }

    protected void commitConnection(Connection connection) {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to commit connection", e);
        }
    }

    @Override
    public int getCurrentVersion(String key) {
        Connection connection = getConnect();
        String query = "SELECT MAX(version_c) FROM key_value_t WHERE key_c = ? AND deleted_c = FALSE";
        int version = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, key);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                version = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return version;
    }

    @Override
    public int getNextVersion(String key) {
        Connection connection = getConnect();
        int version = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT MAX(version_c) FROM " +
            "key_value_t WHERE key_c = ?")) {
            preparedStatement.setString(1, key);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                version = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return version + 1;
    }

    @Override
    public Physical.EncryptedValue read(String key, int version) {
        Connection connection = getConnect();
        String query = "SELECT value_c, encryptor_key_hash_c FROM key_value_t WHERE key_c = ? AND version_c = ? AND " +
            "deleted_c = FALSE";
        Physical.EncryptedValue encryptedValue = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, key);
            preparedStatement.setInt(2, version);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String value = resultSet.getString(1);
                String encryptorKeyHash = resultSet.getString(2);
                encryptedValue = new Physical.EncryptedValue(value, encryptorKeyHash);
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return encryptedValue;
    }

    @Override
    public void write(String key, String value, int version, String encryptorKeyHash) {
        Connection connection = getConnect();
        String query = "INSERT INTO key_value_t (key_c, value_c, version_c, updated_at_c, encryptor_key_hash_c) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, value);
            preparedStatement.setInt(3, version);
            preparedStatement.setLong(4, System.currentTimeMillis());
            preparedStatement.setString(5, encryptorKeyHash);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public void delete(String key, int version) {
        Connection connection = getConnect();
        String query = "UPDATE key_value_t SET deleted_c = TRUE WHERE key_c = ? AND version_c = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, key);
            preparedStatement.setInt(2, version);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public List<String> listKeys(String key) {
        Connection connection = getConnect();
        String query = "SELECT DISTINCT key_c FROM key_value_t WHERE key_c LIKE ? AND deleted_c = FALSE";
        List<String> keys = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, key + "%");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                keys.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return keys;
    }

    @Override
    public Physical.EncryptedEncryptionKey readEncryptedKey(String encryption_key_hash_c) {
        Connection connection = getConnect();
        String query = "SELECT encrypted_encryption_key_c, encryption_key_hash_c, encryptor_key_hash_c FROM " +
            "encryption_keys_t WHERE encryption_key_hash_c = ?";
        Physical.EncryptedEncryptionKey encryptedEncryptionKey = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, encryption_key_hash_c);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                encryptedEncryptionKey = new Physical.EncryptedEncryptionKey(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3)
                );
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return encryptedEncryptionKey;
    }

    @Override
    public void writeEncryptedKey(Physical.EncryptedEncryptionKey encryptedEncryptionKey) {
        Connection connection = getConnect();
        String query = "INSERT INTO encryption_keys_t (encrypted_encryption_key_c, encryption_key_hash_c, " +
            "encryptor_key_hash_c) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, encryptedEncryptionKey.encryptedEncryptionKey());
            preparedStatement.setString(2, encryptedEncryptionKey.encryptionKeyHash());
            preparedStatement.setString(3, encryptedEncryptionKey.encryptorKeyHash());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public Optional<User> readUser(String username) {
        String query = "SELECT email_c, password_hash_c, password_last_changed_c, roles_c, lastLogin_c, locked_c, " +
            "enabled_c FROM users_t WHERE username_c = ?";
        Connection connection = getConnect();
        User user = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                user = new User();
                user.setUsername(username);
                user.setEmail(resultSet.getString(1));
                user.setPasswordHash(resultSet.getString(2));
                user.setPasswordLastChanged(resultSet.getLong(3));
                user.setRolesString(resultSet.getString(4));
                user.setLastLogin(resultSet.getLong(5));
                user.setLocked(resultSet.getBoolean(6));
                user.setEnabled(resultSet.getBoolean(7));
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return Optional.ofNullable(user);
    }

    @Override
    public void createUser(User user) {
        String query = "INSERT INTO users_t (username_c, email_c, password_hash_c, password_last_changed_c, roles_c, " +
            "lastLogin_c, locked_c, enabled_c) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = getConnect();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getEmail());
            preparedStatement.setString(3, user.getPasswordHash());
            preparedStatement.setLong(4, user.getPasswordLastChanged());
            preparedStatement.setString(5, user.getRolesString());
            preparedStatement.setLong(6, user.getLastLogin());
            preparedStatement.setBoolean(7, user.isLocked());
            preparedStatement.setBoolean(8, user.isEnabled());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public void updateUser(User user) {
        String query = "UPDATE users_t SET email_c = ?, password_hash_c = ?, password_last_changed_c = ?, roles_c = " +
            "?, lastLogin_c = ?, locked_c = ?, enabled_c = ? WHERE username_c = ?";
        Connection connection = getConnect();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, user.getEmail());
            preparedStatement.setString(2, user.getPasswordHash());
            preparedStatement.setLong(3, user.getPasswordLastChanged());
            preparedStatement.setString(4, user.getRolesString());
            preparedStatement.setLong(5, user.getLastLogin());
            preparedStatement.setBoolean(6, user.isLocked());
            preparedStatement.setBoolean(7, user.isEnabled());
            preparedStatement.setString(8, user.getUsername());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }
}
