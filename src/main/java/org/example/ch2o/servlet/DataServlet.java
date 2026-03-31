package org.example.ch2o.servlet;

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

            switch (pathInfo) {
                case "/latest":
                    result = dao.findLatest();
                    break;
                case "/minute":
                    result = dao.findLastMinutes(1);
                    break;
                case "/3minutes":
                    result = dao.findLastMinutes(3);
                    break;
                case "/5minutes":
                    result = dao.findLastMinutes(5);
                    break;
                case "/10minutes":
                    result = dao.findLastMinutes(10);
                    break;
                case "/30minutes":
                    result = dao.findLastMinutes(30);
                    break;
                case "/hour":
                    result = dao.findLastHour();
                    break;
                case "/day":
                    result = dao.findLastDay();
                    break;
                case "/threedays":
                    result = dao.findLastThreeDays();
                    break;
                case "/range":
                    String start = req.getParameter("start");
                    String end = req.getParameter("end");
                    if (start != null && end != null) {
                        result = dao.findByRange(start, end);
                    }
                    break;
                case "/all":
                    int limit = 1000;
                    String limitParam = req.getParameter("limit");
                    if (limitParam != null) {
                        limit = Integer.parseInt(limitParam);
                    }
                    result = dao.findAll(limit);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }

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
}
