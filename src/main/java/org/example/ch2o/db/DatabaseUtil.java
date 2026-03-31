package org.example.ch2o.db;

import java.io.File;
import java.sql.*;

public class DatabaseUtil {

    private static final String DB_DIR = "data";
    private static final String DB_FILE = DB_DIR + "/sensor_data.db";
    private static String dbUrl;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            new File(DB_DIR).mkdirs();
            // 添加 SQLite 参数确保数据及时落地
            // journal_mode=DELETE: 不使用WAL，直接写入主文件（数据立即可见）
            // synchronous=FULL: 确保每次写入都刷盘
            dbUrl = "jdbc:sqlite:" + DB_FILE + "?journal_mode=DELETE&synchronous=FULL";
            System.out.println("Database file: " + new File(DB_FILE).getAbsolutePath());
            initDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private static void initDatabase() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS sensor_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "device_id TEXT NOT NULL, " +
                "collect_time TEXT NOT NULL, " +
                "uptime_seconds INTEGER, " +
                "ch2o_valid INTEGER, " +
                "ch2o_ppb REAL, " +
                "ch2o_ppm REAL, " +
                "ch2o_mgm3 REAL, " +
                "wifi_rssi INTEGER, " +
                "created_at TEXT DEFAULT (datetime('now', 'localtime')))";

        String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_collect_time ON sensor_data(collect_time)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            stmt.execute(createIndexSql);
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
