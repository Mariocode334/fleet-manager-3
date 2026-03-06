package com.fleet.database;

import com.fleet.model.FleetLog;
import com.fleet.util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogRepository {
    private static LogRepository instance;
    private final DatabaseManager dbManager;
    private final AppLogger logger;

    private LogRepository() {
        this.dbManager = DatabaseManager.getInstance();
        this.logger = AppLogger.getInstance();
    }

    public static synchronized LogRepository getInstance() {
        if (instance == null) {
            instance = new LogRepository();
        }
        return instance;
    }

    public void saveLog(FleetLog log) {
        String sql = "INSERT INTO fleet_logs (vehicle_id, log_level, message, expected_sequence, received_sequence, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, log.getDroneId());
            stmt.setString(2, log.getLogLevel());
            stmt.setString(3, log.getMessage());
            if (log.getExpectedSequence() != null) {
                stmt.setInt(4, log.getExpectedSequence());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            if (log.getReceivedSequence() != null) {
                stmt.setInt(5, log.getReceivedSequence());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            stmt.setTimestamp(6, log.getTimestamp() != null ? log.getTimestamp() : new Timestamp(System.currentTimeMillis()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error(String.valueOf(log.getDroneId()), "Failed to save log: " + e.getMessage());
        }
    }

    public void saveLogsBatch(List<FleetLog> logs) {
        if (logs.isEmpty()) return;

        String sql = "INSERT INTO fleet_logs (vehicle_id, log_level, message, expected_sequence, received_sequence, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (FleetLog log : logs) {
                    stmt.setInt(1, log.getDroneId());
                    stmt.setString(2, log.getLogLevel());
                    stmt.setString(3, log.getMessage());
                    if (log.getExpectedSequence() != null) {
                        stmt.setInt(4, log.getExpectedSequence());
                    } else {
                        stmt.setNull(4, Types.INTEGER);
                    }
                    if (log.getReceivedSequence() != null) {
                        stmt.setInt(5, log.getReceivedSequence());
                    } else {
                        stmt.setNull(5, Types.INTEGER);
                    }
                    stmt.setTimestamp(6, log.getTimestamp() != null ? log.getTimestamp() : new Timestamp(System.currentTimeMillis()));
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                logger.error("BATCH", "Failed to save batch logs: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            logger.error("BATCH", "Failed to save batch logs: " + e.getMessage());
        }
    }
}
