package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.exceptions.PhysicalException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LibSQL extends SqlBackend {

    private final String serverUrl;
    private final String token;

    public LibSQL(Map<String, Object> config) {
        this.serverUrl = config.get("serverUrl").toString();
        this.token = config.get("token").toString();
        super.createTable();
    }

    protected Connection getConnect() {
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:dbeaver:libsql:" + serverUrl, null, token);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to connect to database", e);
        }
        return connection;
    }

    @Override
    public int getCurrentVersion(String key) {
        String query = "SELECT MAX(version_c) FROM key_value_t WHERE key_c = '" + key + "' AND deleted_c = FALSE";
        Connection connection = getConnect();
        int version = 0;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
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
        String query = "SELECT MAX(version_c) FROM key_value_t WHERE key_c = '" + key + "'";
        Connection connection = getConnect();
        int version = 0;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
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
    public EncryptedValue read(String key, int version) {
        String query = "SELECT value_c, encryptor_key_hash_c FROM key_value_t WHERE key_c = '" + key + "' AND " +
            "version_c = " + version + " AND deleted_c = FALSE";
        Connection connection = getConnect();
        EncryptedValue encryptedValue = null;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                String value = resultSet.getString(1);
                String encryptorKeyHash = resultSet.getString(2);
                encryptedValue = new EncryptedValue(value, encryptorKeyHash);
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.closeConnection(connection);
        return encryptedValue;
    }

    @Override
    public void write(String key, String value, int version, String encryptorKeyHash) {
        String query = "INSERT INTO key_value_t (key_c, value_c, version_c, updated_at_c, encryptor_key_hash_c) " +
            "VALUES ('"
            + key + "', '"
            + value + "', "
            + version + ", "
            + System.currentTimeMillis() + ", '"
            + encryptorKeyHash + "')";
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public void delete(String key, int version) {
        String query = "UPDATE key_value_t SET deleted_c = TRUE WHERE key_c = '" + key + "' AND version_c = " + version;
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public List<String> listKeys(String key) {
        String query = "SELECT DISTINCT key_c FROM key_value_t WHERE key_c LIKE '" + key + "%' AND deleted_c = FALSE";
        Connection connection = getConnect();
        List<String> keys = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
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
    public EncryptedEncryptionKey readEncryptedKey(String encryption_key_hash_c) {
        String query = "SELECT encrypted_encryption_key_c, encryption_key_hash_c, encryptor_key_hash_c FROM " +
            "encryption_keys_t WHERE encryption_key_hash_c = '" + encryption_key_hash_c + "'";
        Connection connection = getConnect();
        EncryptedEncryptionKey encryptedEncryptionKey = null;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                encryptedEncryptionKey = new EncryptedEncryptionKey(
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
    public void writeEncryptedKey(EncryptedEncryptionKey encryptedEncryptionKey) {
        String query = "INSERT INTO encryption_keys_t (encrypted_encryption_key_c, encryption_key_hash_c, " +
            "encryptor_key_hash_c) VALUES ('"
            + encryptedEncryptionKey.encryptedEncryptionKey() + "', '"
            + encryptedEncryptionKey.encryptionKeyHash() + "', '"
            + encryptedEncryptionKey.encryptorKeyHash() + "')";
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public Optional<User> readUser(String username) {
        String query = "SELECT email_c, password_hash_c, password_last_changed_c, roles_c, lastLogin_c, locked_c, " +
            "enabled_c FROM users_t WHERE username_c = '" + username + "'";
        Connection connection = getConnect();
        User user = null;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                user = new User();
                user.setUsername(username);
                user.setEmail(resultSet.getString(1));
                user.setPasswordHash(resultSet.getString(2));
                user.setPasswordLastChanged(resultSet.getLong(3));
                user.setRolesString(resultSet.getString(4));
                user.setLastLogin(resultSet.getLong(5));
                long locked = resultSet.getLong(6);
                if (locked != 0) {
                    user.setLocked(true);
                }
                long enabled = resultSet.getLong(7);
                if (enabled != 0) {
                    user.setEnabled(true);
                }
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
            "lastLogin_c, locked_c, enabled_c) VALUES ('"
            + user.getUsername() + "', '"
            + user.getEmail() + "', '"
            + user.getPasswordHash() + "', "
            + user.getPasswordLastChanged() + ", '"
            + user.getRolesString() + "', "
            + user.getLastLogin() + ", "
            + user.isLocked() + ", " +
            user.isEnabled() + ")";
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.execute(query);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

    @Override
    public void updateUser(User user) {
        String query =
            "UPDATE users_t SET email_c = '" + user.getEmail() + "', password_hash_c = '" + user.getPasswordHash() +
                "', password_last_changed_c = " + user.getPasswordLastChanged() + ", roles_c = '" + user.getRolesString() +
                "', lastLogin_c = " + user.getLastLogin() + ", locked_c = " + user.isLocked() + ", enabled_c = " + user.isEnabled() +
                " WHERE username_c = '" + user.getUsername() + "'";
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }


    @Override
    protected void closeConnection(Connection connection) {
    }

    @Override
    protected void commitConnection(Connection connection) {
    }
}
