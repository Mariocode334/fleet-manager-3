# Fleet Manager

Sistema de gestión de drones que reciben datos vía MQTT y los almacenan en MySQL.

## Características

- Recepción de mensajes MQTT con vector de estado de drones
- Almacenamiento en MySQL
- Detección de paquetes perdidos
- Carga de secuencias desde base de datos al iniciar
- Dashboard Grafana para visualización
- Simulador de drones para pruebas

## Requisitos

- Java 21
- Maven 3.6+
- Docker y Docker Compose
- Raspberry Pi OS (64-bit) o cualquier distro Linux

## Instalación Rápida

```bash
# 1. Clonar el proyecto
git clone https://github.com/tu-usuario/fleet-manager.git
cd fleet-manager

# 2. Construir y ejecutar
sudo docker-compose up -d --build

# 3. Acceder a los servicios
```

## Servicios

| Servicio | Puerto | URL |
|----------|--------|-----|
| MQTT | 1883 | tcp://localhost:1883 |
| MySQL | 3306 | localhost:3306 |
| Fleet Manager | 8080 | http://localhost:8080 |
| Grafana | 3000 | http://localhost:3000 |

## Configuración

### Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| DB_HOST | mysql | Host de MySQL |
| DB_PORT | 3306 | Puerto de MySQL |
| DB_NAME | fleet_db | Base de datos |
| DB_USER | fleet_user | Usuario MySQL |
| DB_PASSWORD | fleet_password123 | Contraseña MySQL |
| MQTT_BROKER | mosquitto | Broker MQTT |
| MQTT_PORT | 1883 | Puerto MQTT |
| MQTT_TOPIC | v1/state_vector/update | Topic MQTT |

## Uso

### Enviar mensaje de prueba

```bash
sudo docker exec fleet-manager-mosquitto-1 mosquitto_pub -t v1/state_vector/update -m '{
  "vehicleId": 33,
  "sequenceNumber": 1,
  "location": {"latitude": 45.45123, "longitude": 25.25456, "altitude": 2.1},
  "orientation": {"roll": 5.2, "pitch": 3.1, "yaw": 180.0},
  "battery": {"batteryCapacity": 3.2, "batteryPercentage": 0.75},
  "linearSpeed": 55,
  "lastUpdate": '$(date +%s)'
}'
```

### Consultar datos

```bash
# Ver estados de drones
sudo docker exec fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM drone_states;"

# Ver vehículos
sudo docker exec fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM vehicles;"
```

## Simulador de Drones

### Compilar

```bash
mvn clean package -Psimulator -DskipTests
```

### Ejecutar

```bash
# Usar variable de entorno para el broker MQTT
export MQTT_BROKER=tcp://localhost:1883
java -jar target/drone-simulator.jar 33 1
```

### Parámetros

1. `vehicle_id` - Identificador del dron (ej: 33, 44, 55)
2. `sequence_inicial` - Número de secuencia inicial

### Ejecutar múltiples drones

```bash
# Terminal 1
java -jar drone-simulator.jar 33 1

# Terminal 2
java -jar drone-simulator.jar 44 1

# Terminal 3
java -jar drone-simulator.jar 55 1
```

## Grafana - Visualización

### Acceso

- **URL**: http://localhost:3000
- **Usuario**: admin
- **Contraseña**: admin123

### Configurar Data Source

1. **Configuration** → **Data Sources**
2. **+ Add data source**
3. Seleccionar **MySQL**
4. Configurar:
   - Host: `mysql:3306` (desde Docker) o `localhost:3306` (desde Raspberry)
   - Database: `fleet_db`
   - User: `fleet_user`
   - Password: `fleet_password123`
5. **Save & Test**

### Queries de ejemplo

```sql
-- Lista de vehículos
SELECT vehicle_id, name, created_at FROM vehicles

-- Velocidad última hora
SELECT received_at as time, linear_speed 
FROM drone_states 
WHERE received_at > NOW() - INTERVAL 1 HOUR

-- Batería última hora
SELECT received_at as time, battery_percentage * 100 
FROM drone_states 
WHERE received_at > NOW() - INTERVAL 1 HOUR

-- Orientación
SELECT received_at as time, roll, pitch, yaw 
FROM drone_states 
WHERE received_at > NOW() - INTERVAL 1 HOUR
```

## Formato de Mensaje MQTT

```json
{
  "vehicleId": 33,
  "sequenceNumber": 1,
  "location": {
    "latitude": 45.45123,
    "longitude": 25.25456,
    "altitude": 2.1
  },
  "orientation": {
    "roll": 5.2,
    "pitch": 3.1,
    "yaw": 180.0
  },
  "battery": {
    "batteryCapacity": 3.2,
    "batteryPercentage": 0.75
  },
  "linearSpeed": 55,
  "lastUpdate": 1234567890
}
```

## Comandos Docker

```bash
# Ver servicios
sudo docker-compose ps

# Ver logs
sudo docker-compose logs -f

# Detener servicios
sudo docker-compose down

# Reiniciar
sudo docker-compose restart

# Rebuild
sudo docker-compose up -d --build
```

## Portabilidad

El sistema está diseñado para funcionar en cualquier red sin cambios de configuración:

- **Docker**: Usa nombres de servicio internos (`mysql`, `mosquitto`)
- **Simulador**: Por defecto usa `localhost:1883`, configurable via variable `MQTT_BROKER`
- **Puertos expuestos**: MQTT (1883), MySQL (3306), Grafana (3000)

Para conectar drones externos, usa la IP de la Raspberry Pi:
```
192.168.x.x:1883
```

## Estructura del Proyecto

```
fleet-manager/
├── src/main/java/com/fleet/
│   ├── FleetManagerApp.java
│   ├── config/
│   ├── mqtt/
│   ├── database/
│   ├── model/
│   ├── validation/
│   ├── simulator/
│   └── util/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── init.sql
├── mosquitto.conf
└── README.md
```
