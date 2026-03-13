package com.fleet.simulator;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.*;

public class DroneSimulator {
    private static final String MQTT_BROKER = System.getenv("MQTT_BROKER") != null ? System.getenv("MQTT_BROKER") : "tcp://192.168.1.70:1883";
    private static final String TOPIC = System.getenv("MQTT_TOPIC") != null ? System.getenv("MQTT_TOPIC") : "v1/state_vector/update";
    private static final int MESSAGE_INTERVAL = 10000; // 10 segundos
    private static final int TOTAL_DURATION = 300000; // 5 minutos

    private final int vehicleId;
    private final int initialSequence;
    private final Random random;
    private final Gson gson;
    private int currentSequence;

    // Estado actual del dron (valores base que varían suavemente)
    private double baseLatitude;
    private double baseLongitude;
    private double baseAltitude;
    private double baseRoll;
    private double basePitch;
    private double baseYaw;
    private double baseSpeed;
    private double baseBattery;

    public DroneSimulator(int vehicleId, int initialSequence) {
        this.vehicleId = vehicleId;
        this.initialSequence = initialSequence;
        this.currentSequence = initialSequence;
        this.random = new Random();
        this.gson = new GsonBuilder().create();

        // Inicializar valores base aleatorios
        this.baseLatitude = 40.0 + random.nextDouble() * 10; // España
        this.baseLongitude = -5.0 + random.nextDouble() * 10;
        this.baseAltitude = 50.0 + random.nextDouble() * 50;
        this.baseRoll = random.nextDouble() * 30 - 15;
        this.basePitch = random.nextDouble() * 20 - 10;
        this.baseYaw = random.nextDouble() * 360 - 180;
        this.baseSpeed = 30.0 + random.nextDouble() * 40;
        this.baseBattery = 0.7 + random.nextDouble() * 0.3;
    }

    public void start() throws Exception {
        System.out.println("=== Drone Simulator ===");
        System.out.println("Vehicle ID: " + vehicleId);
        System.out.println("Initial Sequence: " + initialSequence);
        System.out.println("MQTT Broker: " + MQTT_BROKER);
        System.out.println("Topic: " + TOPIC);
        System.out.println("========================");

        MqttSimulatorClient mqttClient = new MqttSimulatorClient(MQTT_BROKER, "drone-" + vehicleId);
        mqttClient.connect();

        long startTime = System.currentTimeMillis();
        int messageCount = 0;

        while (System.currentTimeMillis() - startTime < TOTAL_DURATION) {
            // Generar mensaje con valores que varían suavemente
            String message = generateMessage();

            // Publicar
            mqttClient.publish(TOPIC, message);
            messageCount++;

            System.out.println("[" + vehicleId + "] Mensaje enviado #" + messageCount + 
                             " - Seq: " + currentSequence + 
                             " - Lat: " + String.format("%.5f", baseLatitude) +
                             " - Lon: " + String.format("%.5f", baseLongitude) +
                             " - Speed: " + String.format("%.1f", baseSpeed));

            currentSequence++;

            // Actualizar valores base ligeramente para el siguiente mensaje
            updateBaseValues();

            // Esperar 10 segundos
            Thread.sleep(MESSAGE_INTERVAL);
        }

        System.out.println("=== Simulación terminée ===");
        System.out.println("Total mensajes enviados: " + messageCount);

        mqttClient.disconnect();
    }

    private String generateMessage() {
        JsonObject json = new JsonObject();

        // Usar fecha/hora actual del sistema en formato ISO 8601
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String currentDateTime = now.format(formatter);
        
        // También guardar el timestamp Unix para compatibilidad
        long currentTimestamp = System.currentTimeMillis() / 1000;

        json.addProperty("vehicleId", vehicleId);
        json.addProperty("sequenceNumber", currentSequence);
        json.addProperty("lastUpdate", currentTimestamp);
        json.addProperty("timestamp", currentDateTime);  // Nuevo campo con fecha legible

        // Location
        JsonObject location = new JsonObject();
        location.addProperty("latitude", baseLatitude + (random.nextDouble() * 0.001 - 0.0005));
        location.addProperty("longitude", baseLongitude + (random.nextDouble() * 0.001 - 0.0005));
        location.addProperty("altitude", baseAltitude + (random.nextDouble() * 2 - 1));
        json.add("location", location);

        // Orientation
        JsonObject orientation = new JsonObject();
        orientation.addProperty("roll", baseRoll + (random.nextDouble() * 2 - 1));
        orientation.addProperty("pitch", basePitch + (random.nextDouble() * 2 - 1));
        orientation.addProperty("yaw", normalizeAngle(baseYaw + (random.nextDouble() * 5 - 2.5)));
        json.add("orientation", orientation);

        // Battery
        JsonObject battery = new JsonObject();
        battery.addProperty("batteryCapacity", 3.2);
        battery.addProperty("batteryPercentage", Math.min(1.0, baseBattery - 0.001));
        json.add("battery", battery);

        // Linear speed
        json.addProperty("linearSpeed", (int)(baseSpeed + random.nextDouble() * 4 - 2));

        return gson.toJson(json);
    }

    private void updateBaseValues() {
        // Variación pequeña en cada ciclo
        baseLatitude += (random.nextDouble() * 0.0002 - 0.0001);
        baseLongitude += (random.nextDouble() * 0.0002 - 0.0001);
        baseAltitude += random.nextDouble() * 4 - 2;
        
        baseRoll += random.nextDouble() * 2 - 1;
        basePitch += random.nextDouble() * 2 - 1;
        baseYaw += random.nextDouble() * 5 - 2.5;
        baseYaw = normalizeAngle(baseYaw);
        
        baseSpeed += random.nextDouble() * 4 - 2;
        baseSpeed = Math.max(10, Math.min(100, baseSpeed));
        
        baseBattery -= 0.0005; // Consumición gradual
        baseBattery = Math.max(0.1, baseBattery);
    }

    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java -jar drone-simulator.jar <vehicle_id> <initial_sequence>");
            System.out.println("Ejemplo: java -jar drone-simulator.jar 33 1");
            System.exit(1);
        }

        try {
            int vehicleId = Integer.parseInt(args[0]);
            int initialSequence = Integer.parseInt(args[1]);

            DroneSimulator simulator = new DroneSimulator(vehicleId, initialSequence);
            simulator.start();
        } catch (NumberFormatException e) {
            System.err.println("Error: Los parámetros deben ser números enteros");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
