package com.fleet.mqtt;

import com.fleet.config.Config;
import com.fleet.util.AppLogger;
import org.eclipse.paho.client.mqttv3.*;

import java.util.UUID;

public class MqttClientManager implements MqttCallback {
    private static MqttClientManager instance;
    private MqttClient mqttClient;
    private final Config config;
    private final AppLogger logger;
    private final MessageHandler messageHandler;
    private volatile boolean connected = false;
    private volatile boolean reconnecting = false;

    private MqttClientManager() {
        this.config = Config.getInstance();
        this.logger = AppLogger.getInstance();
        this.messageHandler = new MessageHandler();
    }

    public static synchronized MqttClientManager getInstance() {
        if (instance == null) {
            instance = new MqttClientManager();
        }
        return instance;
    }

    public void connect() {
        String clientId = config.getMqttClientId() + "-" + System.currentTimeMillis();
        
        int retryCount = 0;
        while (!connected && !Thread.currentThread().isInterrupted()) {
            try {
                if (mqttClient != null && mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    mqttClient.close();
                }

                mqttClient = new MqttClient(config.getMqttUrl(), clientId, null);
                mqttClient.setCallback(this);

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(30);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);
                options.setMaxInflight(1000);

                logger.info("MQTT", "Connecting to broker: " + config.getMqttUrl());
                mqttClient.connect(options);
                
                connected = true;
                reconnecting = false;
                logger.info("MQTT", "Connected to MQTT broker successfully");

                subscribeToTopic();

            } catch (MqttException e) {
                retryCount++;
                logger.warning("MQTT", "Connection failed (attempt " + retryCount + "): " + e.getMessage());
                connected = false;
                sleep(5000);
            }
        }
    }

    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(config.getMqttTopic(), 1);
            logger.info("MQTT", "Subscribed to topic: " + config.getMqttTopic());
        } catch (MqttException e) {
            logger.error("MQTT", "Failed to subscribe to topic: " + e.getMessage());
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        connected = false;
        logger.warning("MQTT", "Connection lost: " + cause.getMessage());
        
        if (!reconnecting) {
            reconnecting = true;
            logger.info("MQTT", "Attempting to reconnect...");
            connect();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());
            messageHandler.handleMessage(payload);
        } catch (Exception e) {
            logger.error("MQTT", "Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public boolean isConnected() {
        return connected && mqttClient != null && mqttClient.isConnected();
    }

    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
            connected = false;
            logger.info("MQTT", "Disconnected from MQTT broker");
        } catch (MqttException e) {
            logger.error("MQTT", "Error disconnecting: " + e.getMessage());
        }
    }
}
