package com.fleet.model;

import java.sql.Timestamp;

public class FleetLog {
    private Long id;
    private int droneId;
    private String logLevel;
    private String message;
    private Integer expectedSequence;
    private Integer receivedSequence;
    private Timestamp timestamp;

    public FleetLog() {}

    public FleetLog(int droneId, String logLevel, String message, Integer expectedSequence, Integer receivedSequence) {
        this.droneId = droneId;
        this.logLevel = logLevel;
        this.message = message;
        this.expectedSequence = expectedSequence;
        this.receivedSequence = receivedSequence;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getDroneId() { return droneId; }
    public void setDroneId(int droneId) { this.droneId = droneId; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getExpectedSequence() { return expectedSequence; }
    public void setExpectedSequence(Integer expectedSequence) { this.expectedSequence = expectedSequence; }

    public Integer getReceivedSequence() { return receivedSequence; }
    public void setReceivedSequence(Integer receivedSequence) { this.receivedSequence = receivedSequence; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
