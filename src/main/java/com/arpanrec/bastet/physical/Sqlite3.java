package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.exceptions.PhysicalException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class Sqlite3 extends SqlBackend {

    private final String path;

    public Sqlite3(Map<String, Object> config) {
        this.path = config.get("path").toString();
        super.createTable();
    }

    protected Connection getConnect() {
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + this.path);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new PhysicalException("Failed to connect to database", e);
        }
        return connection;
    }

    @Override
    protected void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to close connection", e);
        }
    }

    @Override
    protected void commitConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new PhysicalException("Failed to commit connection", e);
        }
    }
}
