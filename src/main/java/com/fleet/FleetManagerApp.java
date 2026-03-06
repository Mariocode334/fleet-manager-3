package com.fleet;

import com.fleet.config.Config;
import com.fleet.database.DatabaseManager;
import com.fleet.mqtt.MqttClientManager;
import com.fleet.util.AppLogger;

public class FleetManagerApp {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        AppLogger logger = AppLogger.getInstance();
        logger.info("APP", "Fleet Manager starting...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("APP", "Shutdown signal received");
            shutdown();
        }));

        Config config = Config.getInstance();
        logger.info("APP", "Configuration loaded:");
        logger.info("APP", "  - DB: " + config.getJdbcUrl());
        logger.info("APP", "  - MQTT: " + config.getMqttUrl());
        logger.info("APP", "  - Topic: " + config.getMqttTopic());

        if (!waitForDatabase(logger)) {
            logger.error("APP", "Failed to connect to database after multiple attempts");
            System.exit(1);
        }

        MqttClientManager mqttManager = MqttClientManager.getInstance();
        mqttManager.connect();

        logger.info("APP", "Fleet Manager is running and listening for messages...");

        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;  // ← CORREGIDO (sin espacio)
            }
        }
    }

    private static boolean waitForDatabase(AppLogger logger) {
        int maxAttempts = 30;
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                if (dbManager.isConnected()) {
                    logger.info("APP", "Database connected successfully");
                    return true;
                }
            } catch (Exception e) {
                logger.warning("APP", "Database connection attempt " + attempt + " failed: " + e.getMessage());
            }

            logger.info("APP", "Waiting for database... (attempt " + attempt + "/" + maxAttempts + ")");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static void shutdown() {
        running = false;
        AppLogger logger = AppLogger.getInstance();

        try {
            MqttClientManager.getInstance().disconnect();  // ← CORREGIDO (sin espacio)
        } catch (Exception e) {
            logger.error("APP", "Error closing MQTT: " + e.getMessage());
        }

        try {
            DatabaseManager.getInstance().close();
        } catch (Exception e) {
            logger.error("APP", "Error closing database: " + e.getMessage());
        }

        logger.info("APP", "Fleet Manager stopped");
        logger.shutdown();
    }
}
