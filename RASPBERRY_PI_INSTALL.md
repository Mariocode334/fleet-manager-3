# Instalación en Raspberry Pi

Guía completa para instalar Fleet Manager con Grafana, Prometheus y Node Exporter en Raspberry Pi.

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

### 4. Clonar e iniciar proyecto

```bash
cd ~
git clone https://github.com/tu-usuario/fleet-manager.git
cd fleet-manager
sudo docker-compose up -d --build
```

### 5. Instalar Prometheus y Node Exporter (fuera de Docker)

#### Node Exporter

```bash
cd /tmp
wget https://github.com/prometheus/node_exporter/releases/download/v1.6.1/node_exporter-1.6.1.linux-arm64.tar.gz
tar xzf node_exporter-1.6.1.linux-arm64.tar.gz
sudo cp node_exporter-1.6.1.linux-arm64/node_exporter /usr/local/bin/
rm -rf node_exporter-1.6.1.linux-arm64*

# Crear servicio systemd
sudo tee /etc/systemd/system/node_exporter.service > /dev/null <<EOF
[Unit]
Description=Node Exporter
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/node_exporter
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable node_exporter
sudo systemctl start node_exporter
```

#### Prometheus

```bash
cd /tmp
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-arm64.tar.gz
tar xzf prometheus-2.45.0.linux-arm64.tar.gz
sudo cp prometheus-2.45.0.linux-arm64/prometheus /usr/local/bin/
sudo cp prometheus-2.45.0.linux-arm64/promtool /usr/local/bin/
sudo cp -r prometheus-2.45.0.linux-arm64/consoles /etc/prometheus
sudo cp -r prometheus-2.45.0.linux-arm64/console_libraries /etc/prometheus
rm -rf prometheus-2.45.0.linux-arm64*

# Configurar Prometheus
sudo mkdir -p /var/lib/prometheus
sudo tee /etc/prometheus/prometheus.yml > /dev/null <<EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
  - job_name: 'node_exporter'
    static_configs:
      - targets: ['localhost:9100']
EOF

# Crear servicio systemd
sudo tee /etc/systemd/system/prometheus.service > /dev/null <<EOF
[Unit]
Description=Prometheus
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/prometheus --config.file=/etc/prometheus/prometheus.yml --storage.tsdb.path=/var/lib/prometheus
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable prometheus
sudo systemctl start prometheus
```

### 6. Configurar SSH (opcional)

```bash
sudo raspi-config
# Interface Options → SSH → Yes
```

## Servicios Disponibles

| Servicio | Puerto | URL |
|----------|--------|-----|
| MQTT | 1883 | tcp://192.168.x.x:1883 |
| MySQL | 3306 | 192.168.x.x:3306 |
| Fleet Manager | 8080 | http://192.168.x.x:8080 |
| Grafana | 3000 | http://192.168.x.x:3000 |
| Node Exporter | 9100 | http://192.168.x.x:9100 |
| Prometheus | 9090 | http://192.168.x.x:9090 |

## Acceso a Grafana

1. Abrir navegador: **http://192.168.x.x:3000**
2. Usuario: **admin**
3. Contraseña: **admin123**

### Configurar Data Sources

#### MySQL (para dashboards de drones)

1. **Configuration** → **Data Sources**
2. **+ Add data source** → **MySQL**
3. Configurar:
   - Host: `mysql:3306`
   - Database: `fleet_db`
   - User: `fleet_user`
   - Password: `fleet_password123`
4. **Save & Test**

#### Prometheus (para métricas del sistema)

1. **Configuration** → **Data Sources**
2. **+ Add data source** → **Prometheus**
3. Configurar:
   - URL: `http://192.168.1.70:9090` (IP de la Raspberry Pi)
4. **Save & Test**

### Importar Dashboards

1. Ir a **Dashboards** → **Import**
2. Importar los JSON de la carpeta `grafana-provisioning/dashboards/`

Dashboards disponibles:
- **fleet-overview.json** - Vista general de drones (con botones dinámicos)
- **vehicle-detail.json** - Detalle de cada dron
- **system-performance.json** - Métricas del sistema (CPU, RAM, Disco, Red)

### Funcionalidades del Dashboard

**Vista General**:
- Muestra una tabla con todos los vehículos registrados
- **Botones dinámicos**: Al hacer clic en "Ver detalles" se navega al dashboard del dron
- **Actualización automática**: Cuando un nuevo dron envía su primer mensaje, los botones se actualizan automáticamente

## Agregar Nuevos Drones

El sistema valida que el `vehicle_id` exista en la base de datos:

```bash
# 1. Registrar el dron
docker exec fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "INSERT INTO vehicles (vehicle_id, name) VALUES (67, 'DRON_67') ON DUPLICATE KEY UPDATE name='DRON_67';"

# 2. Enviar mensaje (el dashboard se actualiza automáticamente)
docker exec fleet-manager-mosquitto-1 mosquitto_pub -t "v1/state_vector/update" -m '{
  "vehicleId": 67,
  "sequenceNumber": 1,
  "lastUpdate": '$(date +%s)',
  "location": {"latitude": 40.0, "longitude": -3.0, "altitude": 50.0},
  "orientation": {"roll": 0.0, "pitch": 0.0, "yaw": 0.0},
  "battery": {"batteryCapacity": 3.2, "batteryPercentage": 0.8},
  "linearSpeed": 50
}'
```

## Verificar que todo funciona

```bash
# Estado de servicios
sudo systemctl status node_exporter
sudo systemctl status prometheus

# Ver métricas de Node Exporter
curl http://localhost:9100/metrics | head

# Ver métricas de Prometheus
curl http://localhost:9090/metrics | head
```

## Comandos Útiles

```bash
# Docker (Fleet Manager)
sudo docker-compose logs -f
sudo docker-compose restart

# Ver logs del Fleet Manager
docker exec fleet-manager-fleet-manager-1 tail -f /app/logs/fleet-manager.log

# Prometheus
sudo systemctl status prometheus
sudo systemctl restart prometheus

# Node Exporter  
sudo systemctl status node_exporter
sudo systemctl restart node_exporter

# Consultar base de datos
docker exec fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM vehicles;"
docker exec fleet-manager-mysql-1 mysql -ufleet_user -pfleet_password123 fleet_db -e "SELECT * FROM fleet_logs;"

# Probar con datos erróneos - vehicle_id no registrado
docker exec fleet-manager-mosquitto-1 mosquitto_pub -t "v1/state_vector/update" -m '{
  "vehicleId": 999,
  "sequenceNumber": 1,
  "lastUpdate": '$(date +%s)',
  "location": {"latitude": 40.0, "longitude": -3.0, "altitude": 50.0},
  "linearSpeed": 50
}'
```

## Validación de Vehículos

El sistema incluye validación de seguridad:

- **Mensajes rechazados**: Si un mensaje llega con un `vehicle_id` que no existe en la tabla `vehicles`, se rechaza y se registra en los logs
- **Ver errores**: `SELECT * FROM fleet_logs WHERE log_level='ERROR';`

## Nota Importante

- La configuración se mantiene en la SD de la Raspberry Pi
- Si cambias de red, la IP cambiará (acceso solo desde misma red WiFi)
- Los dashboards se guardan en Grafana y en `grafana-provisioning/dashboards/`
