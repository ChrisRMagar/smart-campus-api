package com.smartcampus.application;

import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store for the Smart Campus API.
 *
 * WHY ConcurrentHashMap?
 * JAX-RS resource classes are instantiated per-request by default, but they all
 * share the same static singleton DataStore. Multiple threads can therefore read
 * and write concurrently. ConcurrentHashMap provides thread-safe operations
 * without the bottleneck of a global lock, preventing race conditions and data
 * corruption without sacrificing performance.
 *
 * The reading lists are also wrapped with synchronizedList so that concurrent
 * appends to a single sensor's history are safe.
 */
public class DataStore {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // In-memory collections
    // -------------------------------------------------------------------------

    // Key: room id -> Room object
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    // Key: sensor id -> Sensor object
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Key: sensor id -> ordered list of SensorReadings
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Room accessors
    // -------------------------------------------------------------------------

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public void putRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public void deleteRoom(String id) {
        rooms.remove(id);
    }

    // -------------------------------------------------------------------------
    // Sensor accessors
    // -------------------------------------------------------------------------

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public void putSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Ensure a readings list exists for this sensor
        sensorReadings.putIfAbsent(sensor.getId(),
                java.util.Collections.synchronizedList(new ArrayList<>()));
    }

    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    public void deleteSensor(String id) {
        sensors.remove(id);
        sensorReadings.remove(id);
    }

    // -------------------------------------------------------------------------
    // SensorReading accessors
    // -------------------------------------------------------------------------

    public List<SensorReading> getReadings(String sensorId) {
        return sensorReadings.computeIfAbsent(sensorId,
                k -> java.util.Collections.synchronizedList(new ArrayList<>()));
    }

    public void addReading(String sensorId, SensorReading reading) {
        getReadings(sensorId).add(reading);
    }

    // -------------------------------------------------------------------------
    // Seed data – provides a populated environment for demo/testing
    // -------------------------------------------------------------------------

    private void seedData() {
        // -- Rooms --
        Room r1 = new Room("LIB-301", "Library Quiet Study", 40);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        Room r3 = new Room("HALL-A", "Main Assembly Hall", 200);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // -- Sensors --
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 15.0, "LAB-101");
        Sensor s4 = new Sensor("TEMP-002", "Temperature", "ACTIVE", 19.0, "HALL-A");

        putSensor(s1);
        putSensor(s2);
        putSensor(s3);
        putSensor(s4);

        // Link sensors to rooms
        r1.addSensorId(s1.getId());
        r1.addSensorId(s2.getId());
        r2.addSensorId(s3.getId());
        r3.addSensorId(s4.getId());

        // -- Initial readings --
        long now = System.currentTimeMillis();
        addReading("TEMP-001", new SensorReading(java.util.UUID.randomUUID().toString(), now - 60000, 21.0));
        addReading("TEMP-001", new SensorReading(java.util.UUID.randomUUID().toString(), now - 30000, 22.0));
        addReading("CO2-001",  new SensorReading(java.util.UUID.randomUUID().toString(), now - 45000, 400.0));
    }
}
