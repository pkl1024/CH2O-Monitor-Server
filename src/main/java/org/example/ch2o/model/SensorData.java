package org.example.ch2o.model;

import com.google.gson.annotations.SerializedName;

public class SensorData {

    @SerializedName("device_id")
    private String deviceId;
    private String timestamp;
    @SerializedName("uptime_seconds")
    private Long uptimeSeconds;
    private CH2OData ch2o;
    @SerializedName("wifi_rssi")
    private Integer wifiRssi;

    private Long id;

    public static class CH2OData {
        @SerializedName("valid")
        private Boolean valid;
        @SerializedName("concentration_ppb")
        private Double concentrationPpb;
        @SerializedName("concentration_ppm")
        private Double concentrationPpm;
        @SerializedName("concentration_mgm3")
        private Double concentrationMgm3;

        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public Double getConcentrationPpb() { return concentrationPpb; }
        public void setConcentrationPpb(Double concentrationPpb) { this.concentrationPpb = concentrationPpb; }
        public Double getConcentrationPpm() { return concentrationPpm; }
        public void setConcentrationPpm(Double concentrationPpm) { this.concentrationPpm = concentrationPpm; }
        public Double getConcentrationMgm3() { return concentrationMgm3; }
        public void setConcentrationMgm3(Double concentrationMgm3) { this.concentrationMgm3 = concentrationMgm3; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
    public CH2OData getCh2o() { return ch2o; }
    public void setCh2o(CH2OData ch2o) { this.ch2o = ch2o; }
    public Integer getWifiRssi() { return wifiRssi; }
    public void setWifiRssi(Integer wifiRssi) { this.wifiRssi = wifiRssi; }
}
