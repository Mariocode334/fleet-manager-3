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
    private final String grafanaUrl;
    private final String grafanaUser;
    private final String grafanaPassword;

    private Config() {
        this.dbHost = getEnv("DB_HOST", System.getProperty("os.name").contains("Windows") ? "localhost" : "localhost");
        this.dbPort = Integer.parseInt(getEnv("DB_PORT", "3306"));
        this.dbName = getEnv("DB_NAME", "fleet_db");
        this.dbUser = getEnv("DB_USER", "fleet_user");
        this.dbPassword = getEnv("DB_PASSWORD", "fleet_password123");
        this.mqttBroker = getEnv("MQTT_BROKER", "192.168.1.70");
        this.mqttPort = Integer.parseInt(getEnv("MQTT_PORT", "1883"));
        this.mqttTopic = getEnv("MQTT_TOPIC", "v1/state_vector/update");
        this.mqttClientId = getEnv("MQTT_CLIENT_ID", "fleet-manager");
        this.logPath = System.getenv("LOG_PATH") != null ? System.getenv("LOG_PATH") : 
                       (System.getProperty("os.name").contains("Windows") ? "C:/temp/logs" : "/tmp/fleet-logs");
        this.logLevel = getEnv("LOG_LEVEL", "INFO");
        this.grafanaUrl = getEnv("GRAFANA_URL", "http://grafana:3000");
        this.grafanaUser = getEnv("GRAFANA_USER", "admin");
        this.grafanaPassword = getEnv("GRAFANA_PASSWORD", "admin123");
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
    public String getGrafanaUrl() { return grafanaUrl; }
    public String getGrafanaUser() { return grafanaUser; }
    public String getGrafanaPassword() { return grafanaPassword; }

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                dbHost, dbPort, dbName);
    }

    public String getMqttUrl() {
        return String.format("tcp://%s:%d", mqttBroker, mqttPort);
    }
}
