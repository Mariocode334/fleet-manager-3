package com.fleet.simulator;

import org.eclipse.paho.client.mqttv3.*;

public class MqttSimulatorClient {
    private MqttClient mqttClient;
    private final String broker;
    private final String clientId;

    public MqttSimulatorClient(String broker, String clientId) {
        this.broker = broker;
        this.clientId = clientId;
    }

    public void connect() throws MqttException {
        mqttClient = new MqttClient(broker, clientId, null);
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);

        System.out.println("Conectando a " + broker + "...");
        mqttClient.connect(options);
        System.out.println("Conectado!");
    }

    public void publish(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(1);
        mqttClient.publish(topic, mqttMessage);
    }

    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                System.out.println("Desconectado");
            }
        } catch (MqttException e) {
            System.err.println("Error al desconectar: " + e.getMessage());
        }
    }
}
