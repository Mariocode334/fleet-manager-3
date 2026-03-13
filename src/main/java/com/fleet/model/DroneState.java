package com.fleet.model;

public class DroneState {
    private int vehicleId;
    private int sequenceNumber;
    private Location location;
    private Orientation orientation;
    private Battery battery;
    private int linearSpeed;
    private long lastUpdate;
    private String timestamp;  // Fecha/hora legible en formato ISO 8601
    private String receivedAt;

    public DroneState() {}

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public Orientation getOrientation() { return orientation; }
    public void setOrientation(Orientation orientation) { this.orientation = orientation; }

    public Battery getBattery() { return battery; }
    public void setBattery(Battery battery) { this.battery = battery; }

    public int getLinearSpeed() { return linearSpeed; }
    public void setLinearSpeed(int linearSpeed) { this.linearSpeed = linearSpeed; }

    public long getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(long lastUpdate) { this.lastUpdate = lastUpdate; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String receivedAt) { this.receivedAt = receivedAt; }

    public String getDroneId() {
        return String.valueOf(vehicleId);
    }
}
