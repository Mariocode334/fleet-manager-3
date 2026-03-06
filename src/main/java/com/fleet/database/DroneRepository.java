package com.fleet.database;

import com.fleet.model.DroneState;
import com.fleet.util.AppLogger;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DroneRepository {
    private static DroneRepository instance;
    private final DatabaseManager dbManager;
    private final AppLogger logger;

    private DroneRepository() {
        this.dbManager = DatabaseManager.getInstance();
        this.logger = AppLogger.getInstance();
    }

    public static synchronized DroneRepository getInstance() {
        if (instance == null) {
            instance = new DroneRepository();
        }
        return instance;
    }

    public boolean saveDroneState(DroneState state) {
        String sql = "INSERT IGNORE INTO drone_states " +
                "(last_update, vehicle_id, sequence_number, latitude, longitude, altitude, " +
                "battery_capacity, battery_percentage, roll, pitch, yaw, linear_speed, received_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ensureVehicleExists(conn, state.getVehicleId());

            stmt.setTimestamp(1, Timestamp.from(Instant.ofEpochSecond(state.getLastUpdate())));
            stmt.setInt(2, state.getVehicleId());
            stmt.setInt(3, state.getSequenceNumber());

            if (state.getLocation() != null) {
                stmt.setDouble(4, state.getLocation().getLatitude());
                stmt.setDouble(5, state.getLocation().getLongitude());
                stmt.setDouble(6, state.getLocation().getAltitude());
            } else {
                stmt.setNull(4, Types.DOUBLE);
                stmt.setNull(5, Types.DOUBLE);
                stmt.setNull(6, Types.DOUBLE);
            }

            if (state.getBattery() != null) {
                stmt.setDouble(7, state.getBattery().getBatteryCapacity());
                stmt.setDouble(8, state.getBattery().getBatteryPercentage());
            } else {
                stmt.setNull(7, Types.DOUBLE);
                stmt.setNull(8, Types.DOUBLE);
            }

            if (state.getOrientation() != null) {
                stmt.setDouble(9, state.getOrientation().getRoll());
                stmt.setDouble(10, state.getOrientation().getPitch());
                stmt.setDouble(11, state.getOrientation().getYaw());
            } else {
                stmt.setNull(9, Types.DOUBLE);
                stmt.setNull(10, Types.DOUBLE);
                stmt.setNull(11, Types.DOUBLE);
            }

            stmt.setInt(12, state.getLinearSpeed());
            stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info(String.valueOf(state.getVehicleId()), 
                    String.format("State saved: seq=%d, speed=%d, lastUpdate=%d", 
                        state.getSequenceNumber(), state.getLinearSpeed(), state.getLastUpdate()));
                return true;
            }
            return false;

        } catch (SQLException e) {
            logger.error(String.valueOf(state.getVehicleId()), "Failed to save drone state: " + e.getMessage());
            return false;
        }
    }

    private void ensureVehicleExists(Connection conn, int vehicleId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM vehicles WHERE vehicle_id = ?";
        String insertSql = "INSERT IGNORE INTO vehicles (vehicle_id, name, created_at) VALUES (?, ?, ?)";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, vehicleId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, vehicleId);
                    insertStmt.setString(2, "DRON_" + vehicleId);
                    insertStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();
                    logger.info(String.valueOf(vehicleId), "New vehicle registered");
                }
            }
        }
    }

    public void updatePacketLossTracking(int vehicleId, int lastReceived, int currentReceived, int lostCount) {
        String sql = "INSERT INTO packet_loss_tracking " +
                "(vehicle_id, last_received_sequence, current_received_sequence, lost_packets_count, last_checked) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "last_received_sequence = VALUES(last_received_sequence), " +
                "current_received_sequence = VALUES(current_received_sequence), " +
                "lost_packets_count = VALUES(lost_packets_count), " +
                "last_checked = VALUES(last_checked)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);
            stmt.setInt(2, lastReceived);
            stmt.setInt(3, currentReceived);
            stmt.setInt(4, lostCount);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error(String.valueOf(vehicleId), "Failed to update packet loss tracking: " + e.getMessage());
        }
    }
}
