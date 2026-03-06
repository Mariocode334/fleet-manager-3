package com.fleet.model;

public class Battery {
    private double batteryCapacity;
    private double batteryPercentage;

    public Battery() {}

    public Battery(double batteryCapacity, double batteryPercentage) {
        this.batteryCapacity = batteryCapacity;
        this.batteryPercentage = batteryPercentage;
    }

    public double getBatteryCapacity() { return batteryCapacity; }
    public void setBatteryCapacity(double batteryCapacity) { this.batteryCapacity = batteryCapacity; }

    public double getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(double batteryPercentage) { this.batteryPercentage = batteryPercentage; }
}
