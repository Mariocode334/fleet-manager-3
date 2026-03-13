package com.fleet.util;

import com.fleet.config.Config;
import com.fleet.database.DatabaseManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Servicio para actualizar automáticamente el dashboard de Grafana
 * cuando se añade un nuevo dron a la base de datos
 */
public class GrafanaDashboardUpdater {
    private static GrafanaDashboardUpdater instance;
    private final String grafanaUrl;
    private final String grafanaUser;
    private final String grafanaPassword;
    private final DatabaseManager dbManager;
    private final HttpClient httpClient;
    private final AppLogger logger;

    private GrafanaDashboardUpdater() {
        Config config = Config.getInstance();
        this.grafanaUrl = config.getGrafanaUrl();
        this.grafanaUser = config.getGrafanaUser();
        this.grafanaPassword = config.getGrafanaPassword();
        this.dbManager = DatabaseManager.getInstance();
        this.httpClient = HttpClient.newHttpClient();
        this.logger = AppLogger.getInstance();
    }

    public static synchronized GrafanaDashboardUpdater getInstance() {
        if (instance == null) {
            instance = new GrafanaDashboardUpdater();
        }
        return instance;
    }

    /**
     * Actualiza el dashboard de Grafana con los botones para todos los drones
     */
    public void updateDashboard() {
        try {
            // Obtener vehículos de la base de datos
            String sql = "SELECT vehicle_id, name FROM vehicles ORDER BY vehicle_id";
            StringBuilder rows = new StringBuilder();
            
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    int vid = rs.getInt("vehicle_id");
                    String name = rs.getString("name");
                    rows.append("<tr><td style='padding:10px;border:1px solid #ddd;'>")
                        .append(name)
                        .append("</td><td style='padding:10px;border:1px solid #ddd;text-align:center;'><a href='/d/vehicle-detail?var-selected_vehicle=")
                        .append(vid)
                        .append("' style='background:#5bc0de;color:white;padding:8px 15px;text-decoration:none;border-radius:4px;font-size:14px;'>Ver detalles</a></td></tr>");
                }
            }

            String tableHtml = "<table style='width:100%;border-collapse:collapse;'><tr style='background:#5bc0de;color:white;'><th style='padding:12px;border:1px solid #ddd;text-align:left;'>Dron</th><th style='padding:12px;border:1px solid #ddd;'>Acción</th></tr>" + rows.toString() + "</table>";

            // Crear payload JSON
            String jsonPayload = String.format("""
                {
                    "dashboard": {
                        "id": 1,
                        "uid": "fleet-overview",
                        "title": "Fleet Manager - Vista General",
                        "tags": ["fleet-manager", "drones"],
                        "timezone": "browser",
                        "schemaVersion": 38,
                        "refresh": "5s",
                        "panels": [
                            {
                                "id": 1,
                                "title": "Vehículos del Fleet",
                                "type": "table",
                                "gridPos": {"h": 12, "w": 24, "x": 0, "y": 0},
                                "targets": [{
                                    "refId": "A",
                                    "datasource": {"type": "mysql", "uid": "e1f854f9-ef52-465a-a969-1a3948ea6096"},
                                    "rawSql": "SELECT vehicle_id as 'ID', name as 'Dron', COALESCE(COUNT(ds.id), 0) as 'Mensajes' FROM vehicles v LEFT JOIN drone_states ds ON v.vehicle_id = ds.vehicle_id GROUP BY v.vehicle_id ORDER BY vehicle_id",
                                    "format": "table"
                                }],
                                "options": {"showHeader": true}
                            },
                            {
                                "id": 2,
                                "title": "Ver Detalles de cada Dron",
                                "type": "text",
                                "gridPos": {"h": 8, "w": 24, "x": 0, "y": 12},
                                "options": {"content": "%s", "mode": "html"}
                            }
                        ],
                        "templating": {
                            "list": [{
                                "name": "selected_vehicle",
                                "type": "query",
                                "query": "SELECT vehicle_id FROM vehicles ORDER BY vehicle_id",
                                "definition": "SELECT vehicle_id FROM vehicles ORDER BY vehicle_id",
                                "multi": false,
                                "includeAll": false,
                                "hide": 2
                            }]
                        }
                    },
                    "overwrite": true
                }
                """, tableHtml.replace("\"", "\\\"").replace("\n", ""));

            // Enviar a Grafana
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(grafanaUrl + "/api/dashboards/db"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            String auth = grafanaUser + ":" + grafanaPassword;
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
            
            request = HttpRequest.newBuilder()
                    .uri(URI.create(grafanaUrl + "/api/dashboards/db"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + encodedAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("GRAFANA", "Dashboard actualizado automáticamente");
            } else {
                logger.warning("GRAFANA", "Error al actualizar dashboard: " + response.statusCode());
            }

        } catch (Exception e) {
            logger.error("GRAFANA", "Error actualizando dashboard: " + e.getMessage());
        }
    }

    /**
     * Método para verificar si un vehicle_id nuevo y actualizar dashboard si es necesario
     */
    public void checkAndUpdateForNewVehicle(int vehicleId) {
        // Solo actualizar si es un vehicle_id que no existía antes
        // Esto se verifica en el MessageHandler antes de llamar a este método
        updateDashboard();
    }
}
