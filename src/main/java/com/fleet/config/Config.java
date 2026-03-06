package com.fleet.config;

import java.util.Optional;

public class Config {
    private static Config instance;

    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final String mqttBroker;
    private final int mqttPort;
    private final String mqttTopic;
    private final String mqttClientId;
    private final String logPath;
    private final String logLevel;

    private Config() {
        this.dbHost = getEnv("DB_HOST", "mysql");
        this.dbPort = Integer.parseInt(getEnv("DB_PORT", "3306"));
        this.dbName = getEnv("DB_NAME", "fleet_db");
        this.dbUser = getEnv("DB_USER", "fleet_user");
        this.dbPassword = getEnv("DB_PASSWORD", "fleet_password123");
        this.mqttBroker = getEnv("MQTT_BROKER", "mosquitto");
        this.mqttPort = Integer.parseInt(getEnv("MQTT_PORT", "1883"));
        this.mqttTopic = getEnv("MQTT_TOPIC", "v1/state_vector/update");
        this.mqttClientId = getEnv("MQTT_CLIENT_ID", "fleet-manager");
        this.logPath = getEnv("LOG_PATH", "/app/logs");
        this.logLevel = getEnv("LOG_LEVEL", "INFO");
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private String getEnv(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
    }

    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getMqttBroker() { return mqttBroker; }
    public int getMqttPort() { return mqttPort; }
    public String getMqttTopic() { return mqttTopic; }
    public String getMqttClientId() { return mqttClientId; }
    public String getLogPath() { return logPath; }
    public String getLogLevel() { return logLevel; }

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                dbHost, dbPort, dbName);
    }

    public String getMqttUrl() {
        return String.format("tcp://%s:%d", mqttBroker, mqttPort);
    }
}
