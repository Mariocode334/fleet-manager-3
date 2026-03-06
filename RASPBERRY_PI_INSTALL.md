# Instalación en Raspberry Pi 4 (64-bit)

Esta guía detalla todos los pasos para instalar y ejecutar el Fleet Manager en una Raspberry Pi 4 con Raspberry Pi OS de 64 bits.

## Requisitos del Sistema

- **Hardware**: Raspberry Pi 4 (4GB RAM mínimo recomendado)
- **SO**: Raspberry Pi OS (64-bit) - Debian Bookworm
- **Almacenamiento**: Tarjeta microSD de 32GB+ o SSD USB

## 1. Actualizar el Sistema

```bash
sudo apt update && sudo apt upgrade -y
sudo reboot
```

## 2. Instalar Java 11 (OpenJDK)

```bash
sudo apt install -y openjdk-17-jdk
java -version
```

**Nota**: Se recomienda Java 17 por mejor rendimiento y soporte en Raspberry Pi OS actual.

## 3. Instalar Docker

```bash
# Instalar dependencias
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release

# Añadir clave GPG de Docker
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Añadir repositorio Docker
echo "deb [arch=arm64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalar Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Habilitar Docker al inicio
sudo systemctl enable docker
sudo systemctl start docker

# Añadir usuario al grupo docker
sudo usermod -aG docker $USER
# Cerrar sesión y volver a entrar para aplicar cambios
```

## 4. Instalar Git (si no está instalado)

```bash
sudo apt install -y git
```

## 5. Clonar el Proyecto

```bash
cd ~
git clone https://github.com/tu-usuario/fleet-manager.git
cd fleet-manager
```

## 6. Construir la Aplicación (Opcional - Local)

Si prefieres compilar localmente en lugar de usar Docker:

```bash
# Instalar Maven
sudo apt install -y maven

# Compilar
mvn clean package -DskipTests

# El JAR estará en target/fleet-manager-1.0.0.jar
```

## 7. Configurar y Ejecutar con Docker Compose

```bash
# Crear y levantar los contenedores
sudo docker-compose up -d --build

# Ver logs
sudo docker-compose logs -f fleet-manager

# Ver estado
sudo docker-compose ps
```

## 8. Verificar la Instalación

### Verificar MySQL
```bash
sudo docker exec -it fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SHOW TABLES;"
```

### Verificar MQTT
```bash
# Suscribirse al topic
sudo docker exec -it fleet-manager-mosquitto-1 mosquitto_sub -t v1/state_vector/update -v
```

### Verificar Fleet Manager
```bash
sudo docker logs fleet-manager-fleet-manager-1
```

## 9. Probar el Sistema

### Publicar un mensaje de prueba
```bash
sudo docker exec -it fleet-manager-mosquitto-1 mosquitto_pub -t v1/state_vector/update -m '{
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

### Consultar datos en MySQL
```bash
sudo docker exec -it fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM drone_states;"
```

## 10. Comandos Útiles

```bash
# Detener servicios
sudo docker-compose down

# Reiniciar servicios
sudo docker-compose restart

# Ver logs en tiempo real
sudo docker-compose logs -f

# Rebuild después de cambios
sudo docker-compose up -d --build

# Ver uso de recursos
docker stats
```

## 11. Configuración de Red (Opcional)

Si necesitas acceder desde otros dispositivos:

### Abrir puertos en el firewall
```bash
sudo ufw allow 1883/tcp  # MQTT
sudo ufw allow 3306/tcp  # MySQL (solo si es necesario)
sudo ufw enable
```

### Configurar IP estática (opcional)
Editar `/etc/dhcpcd.conf`:
```
interface eth0
static ip_address=192.168.1.100/24
static routers=192.168.1.1
static domain_name_servers=192.168.1.1
```

## 12. Optimizaciones para Raspberry Pi

### Aumentar memoria swap (si es necesario)
```bash
sudo dphys-swapfile swapoff
sudo nano /etc/dphys-swapfile  # Cambiar CONF_SWAPSIZE=1024
sudo dphys-swapfile setup
sudo dphys-swapfile swapon
```

### Configurar Docker para mejor rendimiento
Crear `/etc/docker/daemon.json`:
```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2"
}
```
```bash
sudo systemctl restart docker
```

## 13. Solución de Problemas

### Docker no inicia
```bash
sudo systemctl status docker
sudo journalctl -xe
```

### MySQL no conecta
```bash
sudo docker logs fleet-manager-mysql-1
```

### MQTT no conecta
```bash
sudo docker logs fleet-manager-mosquitto-1
```

### Verificar conectividad entre contenedores
```bash
sudo docker exec fleet-manager-fleet-manager-1 ping -c 3 mysql
sudo docker exec fleet-manager-fleet-manager-1 ping -c 3 mosquitto
```

## Estructura de Archivos Final

```
fleet-manager/
├── src/main/java/com/fleet/
│   ├── FleetManagerApp.java
│   ├── config/
│   ├── mqtt/
│   ├── database/
│   ├── model/
│   ├── validation/
│   └── util/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── init.sql
├── mosquitto.conf
├── README.md
└── examples/
```

## Notas Adicionales

1. **Topic MQTT**: `v1/state_vector/update` (configurable via variable de entorno)
2. **Formato de mensaje**: El sistema acepta el formato StateVector definido en JSON Schema
3. **Base de datos**: MySQL 8.0 con tablas optimizadas para índices
4. **Logging**: Los logs se guardan en `/app/logs/fleet-manager.log` dentro del contenedor

## Acceso desde el Dron

El dron debe configurarse para enviar mensajes MQTT a:
- **Host**: IP de la Raspberry Pi
- **Puerto**: 1883
- **Topic**: `v1/state_vector/update`

Ejemplo de configuración en el dron:
```
MQTT_BROKER=192.168.1.100
MQTT_PORT=1883
MQTT_TOPIC=v1/state_vector/update
```
