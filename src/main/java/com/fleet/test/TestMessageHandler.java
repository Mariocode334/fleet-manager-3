package com.fleet.test;

import com.fleet.mqtt.MessageHandler;

/**
 * Test script para probar el MessageHandler con datos válidos y erróneos
 */
public class TestMessageHandler {
    
    public static void main(String[] args) {
        MessageHandler handler = new MessageHandler();
        
        System.out.println("=== PRUEBA 1: Vehicle ID válido (33) - DEBERÍA aceptarse ===");
        String validMessage = """
            {
                "vehicleId": 33,
                "sequenceNumber": 1,
                "lastUpdate": """ + (System.currentTimeMillis() / 1000) + """
            }
            """;
        handler.handleMessage(validMessage);
        
        System.out.println("\n=== PRUEBA 2: Vehicle ID inválido (999) - DEBERÍA rechazarse ===");
        String invalidVehicleMessage = """
            {
                "vehicleId": 999,
                "sequenceNumber": 1,
                "lastUpdate": """ + (System.currentTimeMillis() / 1000) + """
            }
            """;
        handler.handleMessage(invalidVehicleMessage);
        
        System.out.println("\n=== PRUEBA 3: JSON inválido - DEBERÍA rechazarse ===");
        String invalidJson = "Esto no es JSON válido";
        handler.handleMessage(invalidJson);
        
        System.out.println("\n=== PRUEBA 4: Mensaje vacío - DEBERÍA rechazarse ===");
        handler.handleMessage("");
        handler.handleMessage(null);
        
        System.out.println("\n=== PRUEBA 5: Vehicle ID negativo (-1) - DEBERÍA rechazarse ===");
        String negativeVehicleMessage = """
            {
                "vehicleId": -1,
                "sequenceNumber": 1,
                "lastUpdate": """ + (System.currentTimeMillis() / 1000) + """
            }
            """;
        handler.handleMessage(negativeVehicleMessage);
        
        System.out.println("\n=== PRUEBA 6: Vehicle ID con formato inválido (texto) - DEBERÍA rechazarse ===");
        String textVehicleMessage = """
            {
                "vehicleId": "DRON_33",
                "sequenceNumber": 1,
                "lastUpdate": """ + (System.currentTimeMillis() / 1000) + """
            }
            """;
        handler.handleMessage(textVehicleMessage);
        
        System.out.println("\n=== PRUEBAS COMPLETADAS ===");
    }
}
