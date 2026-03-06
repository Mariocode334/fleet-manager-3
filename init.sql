-- Create database schema for Fleet Manager (State Vector format)

-- Vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT UNIQUE NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_id (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Drone states table (StateVector)
CREATE TABLE IF NOT EXISTS drone_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    last_update TIMESTAMP NOT NULL,
    vehicle_id INT NOT NULL,
    sequence_number INT NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    altitude DOUBLE,
    battery_capacity DOUBLE,
    battery_percentage DOUBLE,
    roll DOUBLE,
    pitch DOUBLE,
    yaw DOUBLE,
    linear_speed INT,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_vehicle_sequence (vehicle_id, sequence_number),
    UNIQUE KEY idx_last_update (last_update),
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_sequence (sequence_number),
    INDEX idx_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fleet logs table
CREATE TABLE IF NOT EXISTS fleet_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT,
    log_level VARCHAR(20),
    message TEXT,
    expected_sequence INT,
    received_sequence INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_log_level (log_level),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Packet loss tracking table
CREATE TABLE IF NOT EXISTS packet_loss_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    last_received_sequence INT,
    current_received_sequence INT,
    lost_packets_count INT,
    last_checked TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_vehicle_tracking (vehicle_id),
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_last_checked (last_checked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
