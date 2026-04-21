package com.smartcampus.models;

/**
 * Represents a historical reading captured by a sensor.
 * Each reading is an immutable data point in the sensor's history.
 */
public class SensorReading {

    private String id;        // Unique reading event ID (UUID recommended)
    private long timestamp;   // Epoch time (ms) when the reading was captured
    private double value;     // The actual metric value recorded by the hardware

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SensorReading{id='" + id + "', timestamp=" + timestamp + ", value=" + value + "}";
    }
}
