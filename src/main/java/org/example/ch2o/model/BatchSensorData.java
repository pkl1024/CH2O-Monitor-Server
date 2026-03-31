package org.example.ch2o.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BatchSensorData {

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("batch_size")
    private Integer batchSize;

    private List<SensorSample> samples;

    public static class SensorSample {
        private String timestamp;

        @SerializedName("uptime_ms")
        private Long uptimeMs;

        private SensorData.MQ135Data mq135;
        private SensorData.CH2OData ch2o;

        @SerializedName("wifi_rssi")
        private Integer wifiRssi;

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public Long getUptimeMs() { return uptimeMs; }
        public void setUptimeMs(Long uptimeMs) { this.uptimeMs = uptimeMs; }

        public SensorData.MQ135Data getMq135() { return mq135; }
        public void setMq135(SensorData.MQ135Data mq135) { this.mq135 = mq135; }

        public SensorData.CH2OData getCh2o() { return ch2o; }
        public void setCh2o(SensorData.CH2OData ch2o) { this.ch2o = ch2o; }

        public Integer getWifiRssi() { return wifiRssi; }
        public void setWifiRssi(Integer wifiRssi) { this.wifiRssi = wifiRssi; }
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

    public List<SensorSample> getSamples() { return samples; }
    public void setSamples(List<SensorSample> samples) { this.samples = samples; }
}
