package com.fleet.test;

import com.fleet.simulator.MqttSimulatorClient;

/**
 * Test para enviar mensajes con vehicle_id que NO existe en la base de datos
 */
public class TestRejectUnknownVehicle {
    public static void main(String[] args) {
        String broker = "tcp://192.168.1.70:1883";
        String topic = "v1/state_vector/update";
        
        try {
            MqttSimulatorClient client = new MqttSimulatorClient(broker, "test-unknown-vehicle");
            client.connect();
            
            // Mensaje con vehicle_id = 888 (NO existe en la base de datos)
            String unknownMessage = String.format("""
                {
                    "vehicleId": 888,
                    "sequenceNumber": 1,
                    "lastUpdate": %d,
                    "timestamp": "2026-03-13T09:55:00",
                    "location": {"latitude": 40.0, "longitude": -3.0, "altitude": 50.0},
                    "orientation": {"roll": 0.0, "pitch": 0.0, "yaw": 0.0},
                    "battery": {"batteryCapacity": 3.2, "batteryPercentage": 0.8},
                    "linearSpeed": 50
                }
                """, System.currentTimeMillis() / 1000);
            
            System.out.println("=== Enviando mensaje con vehicle_id=888 (NO EXISTE EN DB) ===");
            client.publish(topic, unknownMessage);
            System.out.println("Mensaje enviado");
            
            // Mensaje con vehicle_id = 33 (SÍ existe)
            String validMessage = String.format("""
                {
                    "vehicleId": 33,
                    "sequenceNumber": 999,
                    "lastUpdate": %d,
                    "timestamp": "2026-03-13T09:55:00",
                    "location": {"latitude": 40.0, "longitude": -3.0, "altitude": 50.0},
                    "orientation": {"roll": 5.0, "pitch": 2.0, "yaw": 180.0},
                    "battery": {"batteryCapacity": 3.2, "batteryPercentage": 0.75},
                    "linearSpeed": 45
                }
                """, System.currentTimeMillis() / 1000);
            
            System.out.println("\n=== Enviando mensaje con vehicle_id=33 (SÍ EXISTE) ===");
            client.publish(topic, validMessage);
            System.out.println("Mensaje enviado");
            
            Thread.sleep(3000);
            
            client.disconnect();
            System.out.println("\n=== PRUEBAS COMPLETADAS ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
