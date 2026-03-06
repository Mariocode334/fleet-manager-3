package com.fleet.model;

public class Orientation {
    private double roll;
    private double pitch;
    private double yaw;

    public Orientation() {}

    public Orientation(double roll, double pitch, double yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public double getRoll() { return roll; }
    public void setRoll(double roll) { this.roll = roll; }

    public double getPitch() { return pitch; }
    public void setPitch(double pitch) { this.pitch = pitch; }

    public double getYaw() { return yaw; }
    public void setYaw(double yaw) { this.yaw = yaw; }
}
