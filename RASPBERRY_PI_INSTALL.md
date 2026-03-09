# Instalación en Raspberry Pi

Guía completa para instalar Fleet Manager en Raspberry Pi.

## Requisitos

- Raspberry Pi 4 (4GB RAM) o Raspberry Pi 5
- Tarjeta microSD 32GB+
- Raspberry Pi OS (64-bit)

## Instalación

### 1. Actualizar sistema

```bash
sudo apt update && sudo apt upgrade -y
```

### 2. Instalar Java 21

```bash
sudo apt install -y openjdk-21-jdk
java -version
```

### 3. Instalar Docker

```bash
sudo apt install -y docker.io docker-compose
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
```

**Nota**: Cerrar sesión y volver a entrar para aplicar cambios del grupo.

### 4. Clonar proyecto

```bash
cd ~
git clone https://github.com/tu-usuario/fleet-manager.git
cd fleet-manager
```

### 5. Configurar SSH (opcional para acceso remoto)

```bash
sudo raspi-config
# Interface Options → SSH → Yes
```

### 6. Ejecutar

```bash
cd fleet-manager
sudo docker-compose up -d --build
```

### 7. Verificar

```bash
sudo docker-compose ps
```

Debería mostrar:
- fleet-manager   | Up
- mosquitto      | Up  
- mysql          | Up (healthy)
- grafana        | Up

## Servicios Disponibles

| Servicio | Puerto | URL |
|----------|--------|-----|
| MQTT | 1883 | tcp://192.168.x.x:1883 |
| MySQL | 3306 | 192.168.x.x:3306 |
| Fleet Manager | 8080 | http://192.168.x.x:8080 |
| Grafana | 3000 | http://192.168.x.x:3000 |

## Acceso a Grafana

1. Abrir navegador: **http://192.168.x.x:3000**
2. Usuario: **admin**
3. Contraseña: **admin123**

### Configurar Data Source

1. **Configuration** → **Data Sources**
2. **+ Add data source**
3. Seleccionar **MySQL**
4. Configurar:
   - Host: `mysql:3306`
   - Database: `fleet_db`
   - User: `fleet_user`
   - Password: `fleet_password123`
5. **Save & Test**

## Probar el Sistema

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

### Ver datos

```bash
sudo docker exec fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM vehicles;"
```

## Simulador de Drones

### Compilar

```bash
mvn clean package -Psimulator -DskipTests
```

### Ejecutar (en la Raspberry Pi)

```bash
# Dron 33
java -jar target/drone-simulator.jar 33 1

# Dron 44 (en otra terminal)
java -jar target/drone-simulator.jar 44 1
```

## Conectar Drones Reales

Los drones deben configurarse para enviar mensajes MQTT a:

- **Broker**: `192.168.x.x` (IP de la Raspberry Pi)
- **Puerto**: `1883`
- **Topic**: `v1/state_vector/update`

## Comandos Útiles

```bash
# Ver logs
sudo docker-compose logs -f

# Detener
sudo docker-compose down

# Reiniciar
sudo docker-compose restart

# Ver estado
sudo docker-compose ps
```

## Solución de Problemas

### Docker no inicia

```bash
sudo systemctl status docker
sudo journalctl -xe
```

### Ver logs de un servicio

```bash
sudo docker logs fleet-manager-fleet-manager-1
sudo docker logs fleet-manager-mysql-1
sudo docker logs fleet-manager-mosquitto-1
```

### Regenerar contenedores

```bash
sudo docker-compose down
sudo docker-compose up -d --build
```

## Portabilidad

El sistema funciona automáticamente en cualquier red:

- No requiere cambios de IP
- Usa nombres de servicio Docker internos
- Los drones se conectan usando la IP de la Raspberry Pi
