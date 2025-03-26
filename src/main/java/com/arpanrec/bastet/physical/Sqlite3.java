package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.exceptions.PhysicalException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sqlite3 extends SqlBackend {

    private String path = "bastet.sqlite3";

    public Sqlite3(Map<String, Object> config) {
        if (config.containsKey("path") && !config.get("path").toString().isEmpty()) {
            this.path = config.get("path").toString();
        }
        createTable();
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
}
