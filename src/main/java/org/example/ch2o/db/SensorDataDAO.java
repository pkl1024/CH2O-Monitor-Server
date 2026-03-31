package org.example.ch2o.db;

import org.example.ch2o.model.SensorData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SensorDataDAO {

    public void save(SensorData data) throws SQLException {
        String sql = "INSERT INTO sensor_data (device_id, collect_time, uptime_seconds, " +
                "ch2o_valid, ch2o_ppb, ch2o_ppm, ch2o_mgm3, wifi_rssi) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.getDeviceId());
            pstmt.setString(2, data.getTimestamp());
            pstmt.setObject(3, data.getUptimeSeconds());

            if (data.getCh2o() != null) {
                pstmt.setObject(4, data.getCh2o().getValid() != null ? (data.getCh2o().getValid() ? 1 : 0) : null);
                pstmt.setObject(5, data.getCh2o().getConcentrationPpb());
                pstmt.setObject(6, data.getCh2o().getConcentrationPpm());
                pstmt.setObject(7, data.getCh2o().getConcentrationMgm3());
            } else {
                pstmt.setNull(4, Types.INTEGER);
                pstmt.setNull(5, Types.REAL);
                pstmt.setNull(6, Types.REAL);
                pstmt.setNull(7, Types.REAL);
            }

            pstmt.setObject(8, data.getWifiRssi());
            pstmt.executeUpdate();
        }
    }

    public SensorData findLatest() throws SQLException {
        String sql = "SELECT * FROM sensor_data ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return extractData(rs);
            }
        }
        return null;
    }

    public List<SensorData> findLastMinutes(int minutes) throws SQLException {
        String sql = "SELECT * FROM sensor_data WHERE collect_time >= datetime('now', '-" + minutes + " minutes', 'localtime') ORDER BY collect_time DESC";
        return queryList(sql);
    }

    public List<SensorData> findLastHour() throws SQLException {
        return findLastMinutes(60);
    }

    public List<SensorData> findLastDay() throws SQLException {
        return findLastMinutes(60 * 24);
    }

    public List<SensorData> findLastThreeDays() throws SQLException {
        return findLastMinutes(60 * 24 * 3);
    }

    public List<SensorData> findByRange(String start, String end) throws SQLException {
        String sql = "SELECT * FROM sensor_data WHERE collect_time BETWEEN ? AND ? ORDER BY collect_time DESC";
        List<SensorData> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start);
            pstmt.setString(2, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractData(rs));
                }
            }
        }
        return list;
    }

    public List<SensorData> findAll(int limit) throws SQLException {
        String sql = "SELECT * FROM sensor_data ORDER BY collect_time DESC LIMIT ?";
        List<SensorData> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractData(rs));
                }
            }
        }
        return list;
    }

    private List<SensorData> queryList(String sql) throws SQLException {
        List<SensorData> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractData(rs));
            }
        }
        return list;
    }

    private SensorData extractData(ResultSet rs) throws SQLException {
        SensorData data = new SensorData();
        data.setId(rs.getLong("id"));
        data.setDeviceId(rs.getString("device_id"));
        data.setTimestamp(rs.getString("collect_time"));
        data.setUptimeSeconds(getLong(rs, "uptime_seconds"));
        data.setWifiRssi(getInt(rs, "wifi_rssi"));

        SensorData.CH2OData ch2o = new SensorData.CH2OData();
        Integer validInt = getInt(rs, "ch2o_valid");
        ch2o.setValid(validInt != null ? validInt == 1 : null);
        ch2o.setConcentrationPpb(getDouble(rs, "ch2o_ppb"));
        ch2o.setConcentrationPpm(getDouble(rs, "ch2o_ppm"));
        ch2o.setConcentrationMgm3(getDouble(rs, "ch2o_mgm3"));
        data.setCh2o(ch2o);

        return data;
    }

    private Integer getInt(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        return null;
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        return null;
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        return null;
    }
}
