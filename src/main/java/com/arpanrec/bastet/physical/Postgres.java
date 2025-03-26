package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.exceptions.PhysicalException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Postgres extends SqlBackend {

    private String host = "127.0.0.1";
    private int port = 5432;
    private String database = "bastet";
    private String user = "bastet";
    private String password = "bastet";

    public Postgres(Map<String, Object> config) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new PhysicalException("Failed to load PostgreSQL JDBC driver", e);
        }
        if (config.containsKey("host") && !config.get("host").toString().isEmpty()) {
            this.host = config.get("host").toString();
        }
        if (config.containsKey("port")) {
            this.port = (int) config.get("port");
        }
        if (config.containsKey("database") && !config.get("database").toString().isEmpty()) {
            this.database = config.get("database").toString();
        }
        if (config.containsKey("user") && !config.get("user").toString().isEmpty()) {
            this.user = config.get("user").toString();
        }
        if (config.containsKey("password") && !config.get("password").toString().isEmpty()) {
            this.password = config.get("password").toString();
        }
        createTable();
    }

    protected Connection getConnect() {
        Connection connection;
        try {
            connection = DriverManager.getConnection(
                "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.database,
                this.user,
                this.password
            );
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to connect to database", e);
        }
        return connection;
    }

    @Override
    protected void createTable() {
        Connection connection = getConnect();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS encryption_keys_t ("
                + "id_c BIGSERIAL, "
                + "encrypted_encryption_key_c TEXT NOT NULL, "
                + "encryption_key_hash_c TEXT NOT NULL UNIQUE, "
                + "encryptor_key_hash_c TEXT NOT NULL, "
                + "CONSTRAINT encryption_keys_t_pk PRIMARY KEY (id_c));");
            statement.execute("CREATE TABLE IF NOT EXISTS key_value_t ("
                + "id_c BIGSERIAL, "
                + "key_c TEXT NOT NULL, "
                + "value_c TEXT NOT NULL, "
                + "deleted_c BOOLEAN DEFAULT FALSE NOT NULL, "
                + "version_c INTEGER NOT NULL, "
                + "updated_at_c BIGINT NOT NULL, "
                + "encryptor_key_hash_c TEXT NOT NULL, "
                + "FOREIGN KEY (encryptor_key_hash_c) REFERENCES encryption_keys_t (encryption_key_hash_c), "
                + "CONSTRAINT key_value_t_pk PRIMARY KEY (id_c), "
                + "CONSTRAINT key_value_t_unique_key_d_version_d UNIQUE (key_c, version_c));");
            statement.execute("CREATE TABLE IF NOT EXISTS users_t ("
                + "id_c BIGSERIAL, "
                + "username_c TEXT NOT NULL UNIQUE, "
                + "email_c TEXT, "
                + "password_hash_c TEXT, "
                + "password_last_changed_c BIGINT, "
                + "roles_c TEXT NOT NULL, "
                + "lastLogin_c BIGINT, "
                + "locked_c BOOLEAN DEFAULT TRUE NOT NULL, "
                + "enabled_c BOOLEAN DEFAULT FALSE NOT NULL, "
                + "CONSTRAINT users_t_pk PRIMARY KEY (id_c));");
            log.info("Created table users_t");
        } catch (SQLException e) {
            throw new PhysicalException("Failed to create statement", e);
        }
        this.commitConnection(connection);
        this.closeConnection(connection);
    }

}
