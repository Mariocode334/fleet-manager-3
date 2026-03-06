# Fleet Manager

Aplicación Java para gestionar drones que envían datos vía MQTT, almacenando la información en MySQL.

## Requisitos

- Java 17
- Maven 3.6+
- Docker y Docker Compose

## Estructura del Proyecto

```
fleet-manager/
├── src/main/java/com/fleet/
│   ├── FleetManagerApp.java          # Clase principal
│   ├── config/
│   │   ├── Config.java               # Carga de variables de entorno
│   │   └── DatabaseConfig.java       # Configuración HikariCP
│   ├── mqtt/
│   │   ├── MqttClientManager.java    # Gestión de conexión MQTT
│   │   └── MessageHandler.java       # Procesamiento de mensajes
│   ├── database/
│   │   ├── DatabaseManager.java      # Gestor de conexiones
│   │   ├── DroneRepository.java      # Operaciones CRUD drones
│   │   └── LogRepository.java        # Guardado de logs
│   ├── model/
│   │   ├── DroneState.java           # Modelo del mensaje StateVector
│   │   ├── Location.java             # Coordenadas GPS
│   │   ├── Orientation.java          # Orientación (roll, pitch, yaw)
│   │   ├── Battery.java              # Información de batería
│   │   └── FleetLog.java             # Modelo de log
│   ├── validation/
│   │   ├── MessageValidator.java     # Validación de mensajes
│   │   └── SequenceTracker.java      # Tracking de secuencias
│   └── util/
│       ├── JsonParser.java           # Parseo JSON con Gson
│       └── AppLogger.java            # Logging a archivo
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── init.sql
├── mosquitto.conf
├── RASPBERRY_PI_INSTALL.md
└── examples/
    ├── test-state-vector-1.json       # Ejemplo mensaje StateVector
    ├── test-state-vector-2.json       # Ejemplo mensaje StateVector
    └── test-state-vector-packet-loss.json # Ejemplo con pérdida de paquetes
```

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| DB_HOST | mysql | Host de MySQL |
| DB_PORT | 3306 | Puerto de MySQL |
| DB_NAME | fleet_db | Nombre de la base de datos |
| DB_USER | fleet_user | Usuario de MySQL |
| DB_PASSWORD | fleet_password123 | Contraseña de MySQL |
| MQTT_BROKER | mosquitto | Broker MQTT |
| MQTT_PORT | 1883 | Puerto MQTT |
| MQTT_TOPIC | v1/state_vector/update | Topic MQTT |
| MQTT_CLIENT_ID | fleet-manager | ID del cliente MQTT |
| LOG_PATH | /app/logs | Directorio de logs |
| LOG_LEVEL | INFO | Nivel de logging |

## Construcción y Ejecución

### Desarrollo Local

```bash
# Compilar
mvn clean package

# Ejecutar
java -jar target/fleet-manager-1.0.0.jar
```

### Docker

```bash
# Construir imagen
docker build -t fleet-manager .

# Ejecutar con docker-compose
docker-compose up -d
```

## docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: fleet_db
      MYSQL_USER: fleet_user
      MYSQL_PASSWORD: fleet_password123
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  mosquitto:
    image: eclipse-mosquitto:2
    ports:
      - "1883:1883"
    volumes:
      - ./mosquitto.conf:/mosquitto/config/mosquitto.conf

  fleet-manager:
    build: .
    depends_on:
      mysql:
        condition: service_healthy
      mosquitto:
        condition: service_started
    environment:
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=fleet_db
      - DB_USER=fleet_user
      - DB_PASSWORD=fleet_password123
      - MQTT_BROKER=mosquitto
      - MQTT_PORT=1883
      - MQTT_TOPIC=v1/state_vector/update
    volumes:
      - fleet_logs:/app/logs

volumes:
  mysql_data:
  fleet_logs:
```

## Formato de Mensaje MQTT

```json
{
  "vehicleId": 33,
  "sequenceNumber": 123,
  "location": {
    "latitude": 45.45123,
    "longitude": 25.25456,
    "altitude": 2.10789
  },
  "orientation": {
    "roll": 45.45123,
    "pitch": 25.25456,
    "yaw": 2.10789
  },
  "battery": {
    "batteryCapacity": 3.2,
    "batteryPercentage": 0.5
  },
  "linearSpeed": 55,
  "lastUpdate": 1588776718
}
```

### Campos Obligatorios

- `vehicleId`: Identificador del vehículo (integer)
- `sequenceNumber`: Número de secuencia (integer)
- `lastUpdate`: Timestamp en segundos desde epoch (integer)
- `location.latitude`: Latitud (-90 a 90)
- `location.longitude`: Longitud (-180 a 180)
- `location.altitude`: Altitud en metros (opcional)
- `orientation.roll`: Rotación roll (-180 a 180)
- `orientation.pitch`: Rotación pitch (-90 a 90)
- `orientation.yaw`: Rotación yaw (-180 a 180)
- `battery.batteryCapacity`: Capacidad de batería en Ah (opcional)
- `battery.batteryPercentage`: Porcentaje de batería (0-1, opcional)
- `linearSpeed`: Velocidad lineal (opcional)

## Pruebas

### Publicar mensaje de prueba a Mosquitto

```bash
# Suscribirse al topic
docker exec -it mosquitto mosquitto_sub -t v1/state_vector/update -v

# Publicar mensaje
docker exec -it mosquitto mosquitto_pub -t v1/state_vector/update -m '{
  "vehicleId": 33,
  "sequenceNumber": 1,
  "location": {
    "latitude": 45.45123,
    "longitude": 25.25456,
    "altitude": 2.10789
  },
  "orientation": {
    "roll": 5.2,
    "pitch": 3.1,
    "yaw": 180.5
  },
  "battery": {
    "batteryCapacity": 3.2,
    "batteryPercentage": 0.75
  },
  "linearSpeed": 55,
  "lastUpdate": 1588776718
}'
```

### Verificar datos en MySQL

```bash
docker exec -it mysql mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM drone_states;"
docker exec -it mysql mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM drones;"
docker exec -it mysql mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM fleet_logs;"
```

### Ver logs de la aplicación

```bash
docker logs fleet_manager-fleet-manager-1
```

## Características

- **Reconexión automática**: MQTT y MySQL se reconectan automáticamente
- **Detección de paquetes perdidos**: Registra cuando faltan secuencias
- **Validación de mensajes**: Verifica estructura, tipos y rangos
- **Thread-safe**: Manejo concurrente de mensajes
- **Graceful shutdown**: Cierra conexiones correctamente al detener
- **Logging**: Archivo de logs con rotación
