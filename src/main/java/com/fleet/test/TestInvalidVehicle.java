package com.fleet.test;

import com.fleet.simulator.MqttSimulatorClient;

/**
 * Test para enviar mensajes con vehicle_id inválido
 */
public class TestInvalidVehicle {
    public static void main(String[] args) {
        String broker = args.length > 0 ? args[0] : "tcp://192.168.1.70:1883";
        String topic = "v1/state_vector/update";
        
        try {
            MqttSimulatorClient client = new MqttSimulatorClient(broker, "test-invalid-vehicle");
            client.connect();
            
            // Mensaje con vehicle_id = 999 (no existe en la base de datos)
            String invalidMessage = String.format("""
                {
                    "vehicleId": 999,
                    "sequenceNumber": 1,
                    "lastUpdate": %d,
                    "timestamp": "2026-03-13T09:50:00",
                    "location": {"latitude": 40.0, "longitude": -3.0, "altitude": 50.0},
                    "orientation": {"roll": 0.0, "pitch": 0.0, "yaw": 0.0},
                    "battery": {"batteryCapacity": 3.2, "batteryPercentage": 0.8},
                    "linearSpeed": 50
                }
                """, System.currentTimeMillis() / 1000);
            
            System.out.println("=== Enviando mensaje con vehicle_id=999 (INVLÁLIDO) ===");
            client.publish(topic, invalidMessage);
            System.out.println("Mensaje enviado");
            
            Thread.sleep(2000);
            
            // Mensaje con vehicle_id = 33 (VÁLIDO)
            String validMessage = String.format("""
                {
                    "vehicleId": 33,
                    "sequenceNumber": 100,
                    "lastUpdate": %d,
                    "timestamp": "2026-03-13T09:50:00",
                    "location": {"latitude": 40.0, "longitude": -3.0, "altitude": 50.0},
                    "orientation": {"roll": 0.0, "pitch": 0.0, "yaw": 0.0},
                    "battery": {"batteryCapacity": 3.2, "batteryPercentage": 0.8},
                    "linearSpeed": 50
                }
                """, System.currentTimeMillis() / 1000);
            
            System.out.println("\n=== Enviando mensaje con vehicle_id=33 (VÁLIDO) ===");
            client.publish(topic, validMessage);
            System.out.println("Mensaje enviado");
            
            Thread.sleep(2000);
            
            client.disconnect();
            System.out.println("\n=== PRUEBAS COMPLETADAS ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
