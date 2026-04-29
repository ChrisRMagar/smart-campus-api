# Smart Campus Sensor & Room Management API

A fully RESTful API for the University of Westminster's "Smart Campus" initiative,
built with **JAX-RS (Jersey 3)** and an embedded **Grizzly HTTP server**.

**Module:** 5COSC022W – Client-Server Architectures  
**Technology:** JAX-RS / Jersey 3 · Jackson · Grizzly2 · Maven  
**Base URI:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Project Structure](#2-project-structure)
3. [Build & Run Instructions](#3-build--run-instructions)
4. [API Endpoints Reference](#4-api-endpoints-reference)
5. [Sample curl Commands](#5-sample-curl-commands)
6. [Report – Conceptual Questions](#6-report--conceptual-questions)

---

## 1. Project Overview

The Smart Campus API manages three core resources:

| Resource | Description |
|---|---|
| **Room** | A physical campus space with a unique ID, name, and capacity |
| **Sensor** | A device deployed inside a room (Temperature, CO2, Occupancy, etc.) |
| **SensorReading** | A historical data point captured by a sensor at a specific timestamp |

### Architecture decisions

- **In-memory storage:** `ConcurrentHashMap` and `Collections.synchronizedList` are used exclusively — no database.
- **Embedded server:** Grizzly2 is bundled inside the fat JAR; no application server required.
- **Sub-resource locator pattern:** `/sensors/{id}/readings` is delegated to a dedicated `SensorReadingResource` class.
- **Exception mappers:** Every error scenario returns structured JSON — no raw stack traces ever reach the client.
- **JAX-RS filter:** A single `LoggingFilter` handles request/response logging for every endpoint.

---

## 2. Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── application/
    │   ├── Main.java                          # Entry point – starts Grizzly server
    │   ├── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
    │   └── DataStore.java                     # Thread-safe singleton in-memory store
    ├── models/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resources/
    │   ├── DiscoveryResource.java             # GET /api/v1
    │   ├── RoomResource.java                  # /api/v1/rooms
    │   ├── SensorResource.java                # /api/v1/sensors
    │   └── SensorReadingResource.java         # /api/v1/sensors/{id}/readings (sub-resource)
    ├── exceptions/
    │   ├── RoomNotEmptyException.java
    │   ├── RoomNotEmptyExceptionMapper.java   # → 409 Conflict
    │   ├── LinkedResourceNotFoundException.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java  # → 422 Unprocessable Entity
    │   ├── SensorUnavailableException.java
    │   ├── SensorUnavailableExceptionMapper.java       # → 403 Forbidden
    │   └── GlobalExceptionMapper.java         # Catch-all → 500 Internal Server Error
    └── filters/
        └── LoggingFilter.java                 # Request & response logging
```

---

## 3. Build & Run Instructions

### Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 11 or higher |
| Apache Maven | 3.6 or higher |

Verify your environment:

```bash
java -version
mvn -version
```

### Step 1 – Clone the repository

```bash
git clone https://github.com/<your-username>/smart-campus-api.git
cd smart-campus-api
```

### Step 2 – Build the fat JAR

```bash
mvn clean package -DskipTests
```

This produces `target/smart-campus-api-1.0.0.jar` — a self-contained executable that bundles Jersey, Grizzly, and Jackson.

### Step 3 – Start the server

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

You should see:

```
INFO: Smart Campus API started successfully!
INFO: Base URI : http://localhost:8080/api/v1
INFO: Discovery: GET http://localhost:8080/api/v1
```

The server runs on **port 8080** and is ready to accept requests immediately.

### Step 4 – Verify the API is running

```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### Stopping the server

Press `CTRL+C` in the terminal where the server is running. The JVM shutdown hook gracefully stops Grizzly.

---

## 4. API Endpoints Reference

### Part 1 – Discovery

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1` | API metadata + hypermedia links (HATEOAS) |

### Part 2 – Room Management

| Method | Path | Description | Success Code |
|---|---|---|---|
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a new room | 201 |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room | 200 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors exist) | 200 |

### Part 3 – Sensor Operations

| Method | Path | Description | Success Code |
|---|---|---|---|
| GET | `/api/v1/sensors` | List all sensors | 200 |
| GET | `/api/v1/sensors?type=CO2` | Filter sensors by type | 200 |
| POST | `/api/v1/sensors` | Register a sensor (validates roomId) | 201 |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor | 200 |
| DELETE | `/api/v1/sensors/{sensorId}` | Remove a sensor | 200 |

### Part 4 – Sensor Readings (Sub-Resource)

| Method | Path | Description | Success Code |
|---|---|---|---|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor | 200 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Append a new reading | 201 |
| GET | `/api/v1/sensors/{sensorId}/readings/{readingId}` | Get a single reading | 200 |

### Part 5 – Error Scenarios

| Scenario | HTTP Status | Exception |
|---|---|---|
| Delete room with sensors | 409 Conflict | `RoomNotEmptyException` |
| POST sensor with invalid roomId | 422 Unprocessable Entity | `LinkedResourceNotFoundException` |
| POST reading to MAINTENANCE sensor | 403 Forbidden | `SensorUnavailableException` |
| Any unexpected runtime error | 500 Internal Server Error | `GlobalExceptionMapper` |
| Resource not found | 404 Not Found | JAX-RS `NotFoundException` |

---

## 5. Sample curl Commands

> All commands assume the server is running on `http://localhost:8080`.  
> Use `-s | python3 -m json.tool` to pretty-print responses.

---

### 1. Discovery – GET /api/v1

```bash
curl -s http://localhost:8080/api/v1
```

**Expected response (200 OK):**
```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "1.0.0",
  "contact": { "name": "Smart Campus Admin", "email": "smartcampus@westminster.ac.uk" },
  "_links": { "self": "/api/v1", "rooms": "/api/v1/rooms", "sensors": "/api/v1/sensors" }
}
```

---

### 2. List all rooms – GET /api/v1/rooms

```bash
curl -s http://localhost:8080/api/v1/rooms
```

**Expected response (200 OK):**
```json
{
  "count": 3,
  "rooms": [
    { "id": "LIB-301", "name": "Library Quiet Study", "capacity": 40, "sensorIds": ["TEMP-001","CO2-001"] },
    { "id": "LAB-101", "name": "Computer Science Lab", "capacity": 30, "sensorIds": ["OCC-001"] },
    { "id": "HALL-A",  "name": "Main Assembly Hall",   "capacity": 200, "sensorIds": ["TEMP-002"] }
  ]
}
```

---

### 3. Create a new room – POST /api/v1/rooms

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CONF-202",
    "name": "Conference Room B",
    "capacity": 20
  }'
```

**Expected response (201 Created):**
```json
{
  "message": "Room created successfully.",
  "room": { "id": "CONF-202", "name": "Conference Room B", "capacity": 20, "sensorIds": [] },
  "_links": { "self": "/api/v1/rooms/CONF-202", "rooms": "/api/v1/rooms" }
}
```

---

### 4. Register a new sensor – POST /api/v1/sensors

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-002",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 380.5,
    "roomId": "CONF-202"
  }'
```

**Expected response (201 Created):**
```json
{
  "message": "Sensor registered successfully.",
  "sensor": { "id": "CO2-002", "type": "CO2", "status": "ACTIVE", "currentValue": 380.5, "roomId": "CONF-202" },
  "_links": { "self": "/api/v1/sensors/CO2-002", "readings": "/api/v1/sensors/CO2-002/readings" }
}
```

---

### 5. Filter sensors by type – GET /api/v1/sensors?type=CO2

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"
```

**Expected response (200 OK):**
```json
{
  "count": 2,
  "filter": { "type": "CO2" },
  "sensors": [
    { "id": "CO2-001", "type": "CO2", "status": "ACTIVE", ... },
    { "id": "CO2-002", "type": "CO2", "status": "ACTIVE", ... }
  ]
}
```

---

### 6. Post a sensor reading – POST /api/v1/sensors/{sensorId}/readings

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 23.7
  }'
```

**Expected response (201 Created):**
```json
{
  "message": "Reading recorded successfully.",
  "reading": { "id": "uuid-here", "timestamp": 1713000000000, "value": 23.7 },
  "sensorCurrentValue": 23.7
}
```

---

### 7. Get all readings for a sensor – GET /api/v1/sensors/{sensorId}/readings

```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

### 8. Get a specific room – GET /api/v1/rooms/{roomId}

```bash
curl -s http://localhost:8080/api/v1/rooms/LIB-301
```

---

### 9. Delete a sensor before deleting its room

```bash
# Step 1: delete sensor from room
curl -s -X DELETE http://localhost:8080/api/v1/sensors/CO2-002

# Step 2: now delete the (empty) room
curl -s -X DELETE http://localhost:8080/api/v1/rooms/CONF-202
```

---

### 10. Demonstrate error – delete a room that still has sensors (409 Conflict)

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**Expected response (409 Conflict):**
```json
{
  "status": 409,
  "error": "ROOM_NOT_EMPTY",
  "message": "Room 'LIB-301' cannot be deleted because it still has 2 sensor(s) assigned...",
  "hint": "Use DELETE /api/v1/sensors/{sensorId} to remove sensors first."
}
```

---

### 11. Demonstrate error – register sensor with invalid roomId (422)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{ "id": "FAKE-001", "type": "Temperature", "roomId": "NONEXISTENT-ROOM" }'
```

**Expected response (422 Unprocessable Entity):**
```json
{
  "status": 422,
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Cannot register sensor: the referenced roomId 'NONEXISTENT-ROOM' does not exist..."
}
```

---

### 12. Demonstrate error – post reading to MAINTENANCE sensor (403)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{ "value": 12.0 }'
```

**Expected response (403 Forbidden):**
```json
{
  "status": 403,
  "error": "SENSOR_UNAVAILABLE",
  "message": "Sensor 'OCC-001' is currently under MAINTENANCE and cannot accept new readings."
}
```

---

## 6. Report – Conceptual Questions

### Part 1.1 – JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new resource class instance for every incoming HTTP request** (per-request scope). Resource classes are NOT singletons unless explicitly annotated with `@Singleton`.

**Impact on in-memory data management:**  
Since instance fields on resource classes are discarded after each request, mutable shared state must **never** be stored as instance fields. All shared data is held in the `DataStore` singleton (`DataStore.getInstance()`), which lives for the entire JVM lifetime. Because multiple requests can arrive concurrently, `DataStore` uses `ConcurrentHashMap` (which provides lock-free thread-safe reads and fine-grained locking on writes) and `Collections.synchronizedList` for reading lists. This prevents race conditions (e.g., two threads simultaneously writing a sensor that ends up partially initialised) and data loss without requiring coarse-grained `synchronized` blocks that would bottleneck throughput.

---

### Part 1.2 – HATEOAS and Hypermedia

**Hypermedia As The Engine Of Application State (HATEOAS)** is considered a hallmark of advanced REST design because it makes the API entirely self-describing and navigable.

Every response in this API includes a `_links` object (e.g., `"readings": "/api/v1/sensors/TEMP-001/readings"`). This means:

1. **Client decoupling:** Clients do not need to hard-code URLs. If the server changes a path, updating the link in the response is sufficient — clients that follow links automatically adapt.
2. **Discoverability:** A developer can explore the entire API starting from the single root `GET /api/v1` endpoint, following links just like browsing a website — no need to read static documentation first.
3. **Reduced errors:** Clients never construct URLs by string concatenation, eliminating a class of bugs.
4. **Evolutionary APIs:** New resources and links can be added to responses without breaking existing clients that ignore unknown fields.

In contrast, static documentation quickly becomes outdated and requires client code changes every time a URL changes.

---

### Part 2.1 – IDs vs Full Objects in List Responses

| Approach | Pros | Cons |
|---|---|---|
| **IDs only** | Tiny payloads, low bandwidth | N+1 problem: client must fire a separate GET per ID |
| **Full objects** | Single round-trip, rich data | Larger payloads for huge collections |

This API returns **full objects** because campus collections are moderate-sized and the single round-trip benefit outweighs bandwidth cost. For truly massive datasets, best practice is a **summary projection** (id + name + status) with `_links.details` per item (HATEOAS), combined with server-side pagination (`?page=1&size=20`).

---

### Part 2.2 – Is DELETE Idempotent?

**Yes.** RFC 9110 defines idempotency as: applying the same request N times produces the same server state as applying it once.

In this implementation:
- **First DELETE on an existing room:** removes the room, returns `200 OK`.
- **Second DELETE on the same (now absent) roomId:** returns `404 Not Found` — but the **server state** (room absent) is identical.
- No data corruption, no duplicate deletions, no side effects occur.

The HTTP response code may differ (200 vs 404) but idempotency refers to **resource state**, not response codes. The DELETE implementation is therefore correctly idempotent per the REST specification.

---

### Part 3.1 – @Consumes(APPLICATION_JSON) and Wrong Content-Type

JAX-RS performs **content negotiation** before dispatching to the resource method. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey cannot locate a `MessageBodyReader` capable of deserialising the body into a `Sensor` object for that media type.

Jersey immediately returns **HTTP 415 Unsupported Media Type** — the resource method is never called. The `GlobalExceptionMapper` is not triggered because this is a framework-level routing decision. The client receives a clear rejection before any business logic runs, which is both efficient and correct.

---

### Part 3.2 – @QueryParam vs Path Segment for Filtering

| Approach | Example | Assessment |
|---|---|---|
| **Query parameter** | `GET /sensors?type=CO2` | ✅ Correct |
| **Path segment** | `GET /sensors/type/CO2` | ❌ Problematic |

**Why query parameters are superior for filtering:**

1. **Semantic correctness:** Query parameters modify the *representation* of a collection, not the resource identity. `/sensors` always refers to the sensor collection; `?type=CO2` narrows the view.
2. **Optionality:** Query parameters are naturally optional. Omitting `?type` returns all sensors — no extra route needed.
3. **Composability:** Multiple filters compose cleanly: `?type=CO2&status=ACTIVE`. Path-based filters require increasingly deep and fragile URL trees.
4. **Routing clarity:** Path segments imply a new sub-resource. `/sensors/type/CO2` could conflict with a sensor whose ID is literally `"type"`.
5. **Cacheability:** The base path `/sensors` remains stable and cacheable regardless of filter values.

---

### Part 4.1 – Sub-Resource Locator Pattern

The Sub-Resource Locator pattern (used for `/sensors/{sensorId}/readings`) delegates path routing to a separate class (`SensorReadingResource`) rather than defining every nested path in one monolithic controller.

**Benefits:**

1. **Single Responsibility:** `SensorResource` handles sensor lifecycle; `SensorReadingResource` handles reading history. Each class has one reason to change.
2. **Manageable complexity:** A large API with 20+ endpoints per resource would produce an unreadable god-class. Separate classes keep each file focused and short.
3. **Independent testability:** `SensorReadingResource` can be unit-tested in isolation by constructing it with a test sensor ID.
4. **Lazy instantiation:** The sub-resource object is only created when the `/readings` path segment is matched, saving resources for requests that never touch readings.
5. **Reusability:** The same sub-resource class could theoretically be reused under different parent paths without modification.

---

### Part 5.2 – Why 422 Rather Than 404?

- **HTTP 404 Not Found** means the *requested URI itself* is unresolvable — the endpoint does not exist.
- **HTTP 422 Unprocessable Entity** means the URI is valid, the JSON is syntactically correct, but the *semantic content* of the payload cannot be processed.

When a client POSTs `{ "roomId": "GHOST-999" }` to `/api/v1/sensors`, the endpoint `/api/v1/sensors` exists and the JSON parses correctly — so 404 is semantically wrong. The problem is **broken referential integrity inside the body**. The server understood everything about the request except that `GHOST-999` does not exist as a room. HTTP 422 precisely communicates "the entity is understood but contains a semantic error," which is exactly the situation.

---

### Part 5.4 – Cybersecurity Risk of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a significant security vulnerability:

1. **Technology fingerprinting:** Class names and import paths (e.g., `org.glassfish.jersey.server.internal...`) reveal the exact framework and version, enabling attackers to search CVE databases for matching exploits.
2. **Code structure mapping:** File names and line numbers allow an attacker to reconstruct the application's internal architecture and identify logic entry points.
3. **Library version disclosure:** Stack frame entries show third-party library versions. Older versions with known RCE (Remote Code Execution) vulnerabilities become immediate targets.
4. **Business logic exposure:** Deep stack traces reveal call chains, exposing the flow of business rules that could be manipulated.
5. **Internal host/IP leakage:** Some exception messages include internal hostnames, IP addresses, or file system paths, enabling network reconnaissance.

The `GlobalExceptionMapper` prevents all of this by returning only a generic message to the client while writing the full exception to the **server-side log**, which is accessible only to authorised operators.

---

### Part 5.5 – JAX-RS Filters vs Manual Logging

| Approach | Assessment |
|---|---|
| Manual `Logger.info()` in every method | ❌ Violates DRY, error-prone, hard to maintain |
| JAX-RS `ContainerRequestFilter` / `ContainerResponseFilter` | ✅ Correct approach |

**Advantages of filter-based logging:**

1. **Consistency:** Every request and response is logged identically. A developer adding a new endpoint cannot accidentally forget logging.
2. **DRY (Don't Repeat Yourself):** One class handles logging for the entire API. Changing the log format requires editing one file.
3. **Separation of Concerns:** Resource classes contain only business logic. The filter pipeline handles cross-cutting infrastructure concerns invisibly.
4. **Testability:** The `LoggingFilter` can be unit-tested independently of any resource method.
5. **Request metadata access:** Filters receive `ContainerRequestContext` and `ContainerResponseContext`, giving access to method, URI, headers, and status codes that would be cumbersome to replicate inside every resource method.
6. **Zero resource method pollution:** Adding or removing logging requires touching zero resource classes.
