package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.exceptions.PhysicalException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

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
    protected void closeConnection(Connection connection) {
    }

    @Override
    protected void commitConnection(Connection connection) {
    }
}
