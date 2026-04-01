package org.example.ch2o.servlet;

import org.example.ch2o.config.DeviceConfig;
import org.example.ch2o.db.SensorDataDAO;
import org.example.ch2o.util.CommonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

@WebServlet("/api/data/*")
public class DataServlet extends HttpServlet {

    private final SensorDataDAO dao = new SensorDataDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CommonUtil.setJsonResponseHeaders(resp);
        resp.setHeader("Access-Control-Allow-Origin", "*");

        String pathInfo = req.getPathInfo();
        OutputStream outputStream = null;
        PrintWriter out = null;

        try {
            Object result = null;

            if (pathInfo == null || pathInfo.equals("/")) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 获取设备ID（即登录ID）
            String deviceId = req.getParameter("device_id");
            String targetDeviceId = req.getParameter("target_device_id");  // 管理员指定查看的设备

            // 接口：获取设备信息
            if ("/devices".equals(pathInfo)) {
                result = handleDevicesRequest(deviceId);
                sendJsonResponse(resp, req, result, outputStream, out);
                return;
            }

            // 验证设备ID
            if (deviceId == null || deviceId.isEmpty()) {
                sendErrorResponse(resp, "缺少设备ID参数");
                return;
            }

            DeviceConfig config = DeviceConfig.getInstance();
            if (!config.isUserExists(deviceId)) {
                sendErrorResponse(resp, "设备未注册");
                return;
            }

            // 确定查询的设备ID
            String queryDeviceId;
            if (config.isAdmin(deviceId)) {
                // 管理员必须指定目标设备
                if (targetDeviceId == null || targetDeviceId.isEmpty()) {
                    sendErrorResponse(resp, "请选择设备");
                    return;
                }
                queryDeviceId = targetDeviceId;
            } else {
                // 普通用户只能查自己的设备
                queryDeviceId = deviceId;
            }

            // 处理数据查询
            switch (pathInfo) {
                case "/latest":
                    result = dao.findLatest(queryDeviceId);
                    break;
                case "/minute":
                    result = dao.findLastMinutes(1, queryDeviceId);
                    break;
                case "/3minutes":
                    result = dao.findLastMinutes(3, queryDeviceId);
                    break;
                case "/5minutes":
                    result = dao.findLastMinutes(5, queryDeviceId);
                    break;
                case "/10minutes":
                    result = dao.findLastMinutes(10, queryDeviceId);
                    break;
                case "/30minutes":
                    result = dao.findLastMinutes(30, queryDeviceId);
                    break;
                case "/hour":
                    result = dao.findLastHour(queryDeviceId);
                    break;
                case "/day":
                    result = dao.findLastDay(queryDeviceId);
                    break;
                case "/threedays":
                    result = dao.findLastThreeDays(queryDeviceId);
                    break;
                case "/range":
                    String start = req.getParameter("start");
                    String end = req.getParameter("end");
                    if (start != null && end != null) {
                        result = dao.findByRange(start, end, queryDeviceId);
                    }
                    break;
                case "/all":
                    int limit = 1000;
                    String limitParam = req.getParameter("limit");
                    if (limitParam != null) {
                        limit = Integer.parseInt(limitParam);
                    }
                    result = dao.findAll(limit, queryDeviceId);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }

            sendJsonResponse(resp, req, result, outputStream, out);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (out == null) {
                out = resp.getWriter();
            }
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }

        if (out != null) {
            out.flush();
            out.close();
        }
    }

    /**
     * 处理设备信息请求
     */
    private Object handleDevicesRequest(String deviceId) {
        DeviceConfig config = DeviceConfig.getInstance();
        Map<String, Object> result = new HashMap<>();

        if (deviceId == null || deviceId.isEmpty()) {
            result.put("success", false);
            result.put("message", "缺少设备ID参数");
            return result;
        }

        if (!config.isUserExists(deviceId)) {
            result.put("success", false);
            result.put("message", "设备未注册");
            return result;
        }

        List<String> accessibleDevices = config.getAccessibleDevices(deviceId);

        result.put("success", true);
        result.put("device_name", config.getDeviceName(deviceId));
        result.put("is_admin", config.isAdmin(deviceId));

        // 构建设备列表（包含名称）
        Map<String, String> devices = new HashMap<>();
        for (String id : accessibleDevices) {
            devices.put(id, config.getDeviceName(id));
        }
        result.put("devices", devices);

        return result;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":false,\"message\":\"" + message + "\"}");
        out.flush();
        out.close();
    }

    /**
     * 发送JSON响应（支持GZIP压缩）
     */
    private void sendJsonResponse(HttpServletResponse resp, HttpServletRequest req, Object result,
                                   OutputStream outputStream, PrintWriter out) throws IOException {
        String json = result != null ? CommonUtil.getGson().toJson(result) : "{}";

        // Check if client accepts GZIP
        String acceptEncoding = req.getHeader("Accept-Encoding");
        boolean useGzip = acceptEncoding != null && acceptEncoding.contains("gzip");

        if (useGzip) {
            resp.setHeader("Content-Encoding", "gzip");
            outputStream = new GZIPOutputStream(resp.getOutputStream());
            out = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"));
        } else {
            out = resp.getWriter();
        }

        out.print(json);
        out.flush();

        if (outputStream != null) {
            outputStream.close();
        }
    }
}
