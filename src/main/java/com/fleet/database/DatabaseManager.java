package com.fleet.database;

import com.fleet.config.DatabaseConfig;
import com.fleet.util.AppLogger;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final DatabaseConfig dbConfig;
    private final AppLogger logger;

    private DatabaseManager() {
        this.dbConfig = DatabaseConfig.getInstance();
        this.logger = AppLogger.getInstance();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dbConfig.getDataSource().getConnection();
    }

    public boolean isConnected() {
        return dbConfig.isConnected();
    }

    public void close() {
        dbConfig.close();
        logger.info("DATABASE", "Database connection pool closed");
    }
}
