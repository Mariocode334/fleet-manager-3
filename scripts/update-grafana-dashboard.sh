#!/bin/bash
# Script para actualizar el dashboard de Grafana cuando se añade un nuevo dron

# Obtener vehículos de la base de datos
VEHICLES=$(mysql -h 192.168.1.70 -ufleet_user -pfleet_password123 fleet_db -N -e "SELECT vehicle_id, name FROM vehicles ORDER BY vehicle_id;" 2>/dev/null)

# Crear filas HTML
ROWS=""
while read -r vid name; do
    ROWS="${ROWS}<tr><td style='padding:10px;border:1px solid #ddd;'>${name}</td><td style='padding:10px;border:1px solid #ddd;text-align:center;'><a href='/d/vehicle-detail?var-selected_vehicle=${vid}' style='background:#5bc0de;color:white;padding:8px 15px;text-decoration:none;border-radius:4px;font-size:14px;'>Ver detalles</a></td></tr>"
done <<< "$VEHICLES"

TABLE_HTML="<table style='width:100%;border-collapse:collapse;'><tr style='background:#5bc0de;color:white;'><th style='padding:12px;border:1px solid #ddd;text-align:left;'>Dron</th><th style='padding:12px;border:1px solid #ddd;'>Acción</th></tr>${ROWS}</table>"

# Crear payload JSON
python3 << PYEOF
import json

payload = {
    "dashboard": {
        "id": 1,
        "uid": "fleet-overview",
        "title": "Fleet Manager - Vista General",
        "tags": ["fleet-manager", "drones"],
        "timezone": "browser",
        "schemaVersion": 38,
        "version": 33,
        "refresh": "5s",
        "panels": [
            {
                "id": 1,
                "title": "Vehículos del Fleet",
                "type": "table",
                "gridPos": {"h": 12, "w": 24, "x": 0, "y": 0},
                "targets": [
                    {
                        "refId": "A",
                        "datasource": {"type": "mysql", "uid": "e1f854f9-ef52-465a-a969-1a3948ea6096"},
                        "rawSql": "SELECT vehicle_id as 'ID', name as 'Dron', COALESCE(COUNT(ds.id), 0) as 'Mensajes' FROM vehicles v LEFT JOIN drone_states ds ON v.vehicle_id = ds.vehicle_id GROUP BY v.vehicle_id ORDER BY vehicle_id",
                        "format": "table"
                    }
                ],
                "options": {"showHeader": True}
            },
            {
                "id": 2,
                "title": "Ver Detalles de cada Dron",
                "type": "text",
                "gridPos": {"h": 8, "w": 24, "x": 0, "y": 12},
                "options": {"content": """${TABLE_HTML}""", "mode": "html"}
            }
        ],
        "templating": {
            "list": [
                {
                    "name": "selected_vehicle",
                    "type": "query",
                    "query": "SELECT vehicle_id FROM vehicles ORDER BY vehicle_id",
                    "definition": "SELECT vehicle_id FROM vehicles ORDER BY vehicle_id",
                    "multi": False,
                    "includeAll": False,
                    "hide": 2
                }
            ]
        }
    },
    "overwrite": True
}

with open('/tmp/dashboard_update.json', 'w') as f:
    json.dump(payload, f)
print("OK")
PYEOF

# Actualizar dashboard en Grafana
curl -s -X POST "http://192.168.1.70:3000/api/dashboards/db" \
  -u "admin:admin123" \
  -H "Content-Type: application/json" \
  -d @/tmp/dashboard_update.json

echo ""
echo "✅ Dashboard actualizado"
