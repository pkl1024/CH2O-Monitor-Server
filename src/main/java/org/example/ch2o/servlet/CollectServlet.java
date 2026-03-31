package org.example.ch2o.servlet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.ch2o.db.DatabaseUtil;
import org.example.ch2o.db.SensorDataDAO;
import org.example.ch2o.model.BatchSensorData;
import org.example.ch2o.model.SensorData;
import org.example.ch2o.util.CommonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/collect")
public class CollectServlet extends HttpServlet {

    private final SensorDataDAO dao = new SensorDataDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CommonUtil.setJsonResponseHeaders(resp);
        CommonUtil.setCorsHeaders(resp);

        Map<String, Object> result = new HashMap<>();
        PrintWriter out = resp.getWriter();

        long startTime = System.currentTimeMillis();
        String requestId = String.valueOf(System.nanoTime());

        System.out.println("[Collect] ====== 收到请求 #" + requestId + " ======");

        try {
            // 1. 读取请求体
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String jsonBody = sb.toString();
            System.out.println("[Collect] 请求体长度: " + jsonBody.length() + " 字节");

            // 2. 解析JSON
            JsonObject jsonObj = JsonParser.parseString(jsonBody).getAsJsonObject();
            System.out.println("[Collect] JSON解析成功");

            int savedCount = 0;

            // 3. 判断是批量数据还是单条数据
            if (jsonObj.has("samples") && jsonObj.has("batch_size")) {
                System.out.println("[Collect] 检测到批量数据格式");

                // 批量数据格式
                BatchSensorData batch = CommonUtil.getGson().fromJson(jsonObj, BatchSensorData.class);
                System.out.println("[Collect] 设备ID: " + batch.getDeviceId() + ", 批量大小: " + batch.getBatchSize());

                if (batch.getSamples() != null && !batch.getSamples().isEmpty()) {
                    System.out.println("[Collect] 开始保存 " + batch.getSamples().size() + " 条样本...");
                    // 使用批量插入和事务处理
                    savedCount = saveBatchWithTransaction(batch);
                    System.out.println("[Collect] 保存完成，成功: " + savedCount + " 条");
                } else {
                    System.out.println("[Collect] 样本列表为空");
                }

                result.put("saved_count", savedCount);
                result.put("message", "批量数据接收成功");
                System.out.println("[Collect] 批量数据已保存: " + batch.getDeviceId() + " - " + savedCount + " 条, 耗时: " + (System.currentTimeMillis() - startTime) + "ms");

            } else {
                System.out.println("[Collect] 检测到单条数据格式");
                // 单条数据格式（向后兼容）
                SensorData data = CommonUtil.getGson().fromJson(jsonObj, SensorData.class);
                dao.save(data);
                savedCount = 1;

                result.put("saved_count", savedCount);
                result.put("message", "数据接收成功");
                System.out.println("[Collect] 数据已保存: " + data.getDeviceId() + " - " + data.getTimestamp());
            }

            result.put("success", true);
            resp.setStatus(HttpServletResponse.SC_OK);
            System.out.println("[Collect] ====== 请求 #" + requestId + " 处理成功 ======");

        } catch (Exception e) {
            System.err.println("[Collect] ====== 请求 #" + requestId + " 处理失败 ======");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "数据保存失败: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        out.print(CommonUtil.getGson().toJson(result));
        out.flush();
    }

    /**
     * 使用事务批量保存数据，大幅提升SQLite性能
     */
    private int saveBatchWithTransaction(BatchSensorData batch) throws SQLException {
        List<SensorData> dataList = new ArrayList<>();
        for (BatchSensorData.SensorSample sample : batch.getSamples()) {
            dataList.add(convertToSensorData(batch.getDeviceId(), sample));
        }

        String sql = "INSERT INTO sensor_data (device_id, collect_time, uptime_seconds, " +
                "mq135_adc, mq135_voltage, mq135_digital, " +
                "ch2o_valid, ch2o_ppb, ch2o_ppm, ch2o_mgm3, wifi_rssi) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int savedCount = 0;
        int failCount = 0;

        // 使用一个连接和事务批量插入
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);

            try {
                for (SensorData data : dataList) {
                    try {
                        // 安全检查必填字段
                        if (data.getDeviceId() == null || data.getTimestamp() == null) {
                            System.err.println("[Collect] 跳过无效数据: deviceId或timestamp为空");
                            failCount++;
                            continue;
                        }

                        pstmt.setString(1, data.getDeviceId());
                        pstmt.setString(2, data.getTimestamp());
                        pstmt.setObject(3, data.getUptimeSeconds());

                        // 处理MQ135数据（允许为null）
                        if (data.getMq135() != null) {
                            pstmt.setObject(4, data.getMq135().getAdcValue());
                            pstmt.setObject(5, data.getMq135().getVoltage());
                            pstmt.setObject(6, data.getMq135().getDigitalValue());
                        } else {
                            pstmt.setNull(4, java.sql.Types.INTEGER);
                            pstmt.setNull(5, java.sql.Types.REAL);
                            pstmt.setNull(6, java.sql.Types.INTEGER);
                        }

                        // 处理CH2O数据（允许为null）
                        if (data.getCh2o() != null) {
                            pstmt.setObject(7, data.getCh2o().getValid() != null ? (data.getCh2o().getValid() ? 1 : 0) : null);
                            pstmt.setObject(8, data.getCh2o().getConcentrationPpb());
                            pstmt.setObject(9, data.getCh2o().getConcentrationPpm());
                            pstmt.setObject(10, data.getCh2o().getConcentrationMgm3());
                        } else {
                            pstmt.setNull(7, java.sql.Types.INTEGER);
                            pstmt.setNull(8, java.sql.Types.REAL);
                            pstmt.setNull(9, java.sql.Types.REAL);
                            pstmt.setNull(10, java.sql.Types.REAL);
                        }

                        pstmt.setObject(11, data.getWifiRssi());
                        pstmt.addBatch();
                        savedCount++;
                    } catch (Exception e) {
                        failCount++;
                        System.err.println("[Collect] 单条数据准备失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // 执行批量插入
                if (savedCount > 0) {
                    pstmt.executeBatch();
                }

                // 提交事务
                conn.commit();

                if (failCount > 0) {
                    System.out.println("[Collect] 批量处理完成: 成功 " + savedCount + " 条, 失败 " + failCount + " 条");
                }

            } catch (Exception e) {
                // 出错回滚
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[Collect] 事务回滚失败: " + ex.getMessage());
                }
                throw e;
            } finally {
                // 恢复自动提交
                conn.setAutoCommit(true);
            }
        }

        return savedCount;
    }

    /**
     * 将Batch样本转换为SensorData
     */
    private SensorData convertToSensorData(String deviceId, BatchSensorData.SensorSample sample) {
        SensorData data = new SensorData();
        data.setDeviceId(deviceId);
        data.setTimestamp(sample.getTimestamp());
        data.setUptimeSeconds(sample.getUptimeMs() != null ? sample.getUptimeMs() / 1000 : null);
        data.setMq135(sample.getMq135());
        data.setCh2o(sample.getCh2o());
        data.setWifiRssi(sample.getWifiRssi());
        return data;
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CommonUtil.setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
