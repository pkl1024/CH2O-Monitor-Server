# CH2O-Monitor-Server

甲醛浓度监测服务器端 - 接收ESP32设备上报的甲醛传感器数据并提供Web可视化界面。

> **相关项目:** [CH2O-Monitor](https://github.com/pkl1024/CH2O-Monitor) - ESP32 客户端，甲醛传感器数据采集与上报

## 项目概述

这是一个轻量级Java Web应用，用于收集ESP32设备上报的甲醛(CH2O)传感器数据。使用纯Servlet 4.0 + SQLite + GSON，部署为WAR文件到Tomcat 9+。

**技术架构:**
- **前端:** 纯HTML + JavaScript，使用Chart.js进行数据可视化，通过AJAX与REST API通信
- **后端:** 基于Servlet的REST API，处理数据收集和查询
- **数据库:** SQLite嵌入式数据库

## 构建和部署

```bash
# 构建WAR文件
mvn clean package

# 输出: target/CH2O-Monitor-Server.war
```

部署方式：将 `target/CH2O-Monitor-Server.war` 复制到Tomcat的 `webapps/` 目录。

## API接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/` | GET | Web仪表盘页面 |
| `/api/collect` | POST | 接收ESP32上报的JSON数据（单条或批量格式） |
| `/api/data/latest` | GET | 获取最新一条数据 |
| `/api/data/minute` | GET | 最近1分钟数据 |
| `/api/data/3minutes` | GET | 最近3分钟数据 |
| `/api/data/5minutes` | GET | 最近5分钟数据 |
| `/api/data/10minutes` | GET | 最近10分钟数据 |
| `/api/data/30minutes` | GET | 最近30分钟数据 |
| `/api/data/hour` | GET | 最近1小时数据 |
| `/api/data/day` | GET | 最近24小时数据 |
| `/api/data/threedays` | GET | 最近3天数据 |
| `/api/data/range?start=&end=` | GET | 时间范围查询（格式: "yyyy-MM-dd HH:mm:ss"） |
| `/api/data/all?limit=` | GET | 获取所有数据（默认limit: 1000） |

## 数据库

- SQLite数据库自动创建在 `data/sensor_data.db`（Tomcat工作目录）
- 表: `sensor_data`，在 `collect_time` 字段上建立索引以加速时间范围查询

## JSON字段命名

项目使用GSON的 `LOWER_CASE_WITH_UNDERSCORES` 字段命名策略。JSON字段使用snake_case（如 `device_id`, `ch2o.concentration_mgm3`），而Java使用camelCase。

## ESP32数据上报格式

**单条数据格式:**
```json
{
  "device_id": "esp32-ch2o-01",
  "timestamp": "2024-01-15 10:30:00",
  "uptime_seconds": 3600,
  "ch2o": {
    "valid": true,
    "concentration_ppb": 80.5,
    "concentration_ppm": 0.0805,
    "concentration_mgm3": 0.098
  },
  "wifi_rssi": -45
}
```

**批量数据格式:**
```json
{
  "device_id": "esp32-ch2o-01",
  "batch_size": 10,
  "samples": [
    {
      "timestamp": "2024-01-15 10:30:00",
      "uptime_ms": 3600000,
      "ch2o": { "valid": true, "concentration_ppb": 80.5, "concentration_ppm": 0.0805, "concentration_mgm3": 0.098 },
      "wifi_rssi": -45
    }
  ]
}
```

## 甲醛健康阈值 (GB/T 18883-2002)

- 正常: ≤0.08 mg/m³
- 注意: 0.08-0.1 mg/m³
- 警告: 0.1-0.3 mg/m³
- 警报: >0.3 mg/m³
