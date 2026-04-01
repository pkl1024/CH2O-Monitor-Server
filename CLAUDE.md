# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Deploy

```bash
# Build WAR file
mvn clean package

# Output: target/CH2O-Monitor-Server.war
# Deploy: Copy to Tomcat webapps/ directory
```

- Java 8, Servlet 4.0, Maven project
- Dependencies: SQLite JDBC, GSON (see pom.xml)
- No automated tests in this project

## Architecture

**Request Flow:**
ESP32 → CollectServlet (/api/collect) → SensorDataDAO → SQLite

**Key Packages:**
- `org.example.ch2o.servlet` - CollectServlet (POST data collection), DataServlet (GET data queries)
- `org.example.ch2o.db` - DatabaseUtil (connection), SensorDataDAO (CRUD)
- `org.example.ch2o.model` - SensorData, BatchSensorData with nested CH2OData

**Database:**
- SQLite file: `data/sensor_data.db` (relative to Tomcat working directory)
- Auto-initialized on startup with `sensor_data` table and index on `collect_time`

**JSON Serialization:**
- Uses GSON with `LOWER_CASE_WITH_UNDERSCORES` naming policy
- Java camelCase fields map to JSON snake_case (e.g., `deviceId` → `device_id`)
- Use `@SerializedName` annotations for explicit field mapping

**Data Formats:**
- Single: `{device_id, timestamp, uptime_seconds, ch2o: {...}, wifi_rssi}`
- Batch: `{device_id, batch_size, samples: [{timestamp, uptime_ms, ch2o, wifi_rssi}]}`
