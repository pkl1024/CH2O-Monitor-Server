package org.example.ch2o.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 设备配置管理类
 * 配置格式: { "userId": "设备名称" }
 * - userId 即为设备ID，用于登录
 * - 设备名称用于前端显示
 * - 值为 "admin" 表示管理员，可查看所有设备
 */
public class DeviceConfig {

    private static final String CONFIG_FILE = "/data/devices.json";
    private static final Gson GSON = new Gson();

    private static DeviceConfig instance;
    private Map<String, String> config;  // userId -> 设备名称
    private long lastModified;
    private File configFile;

    private DeviceConfig() {
        config = new HashMap<>();
        configFile = new File(CONFIG_FILE);
        loadConfig();
    }

    public static synchronized DeviceConfig getInstance() {
        if (instance == null) {
            instance = new DeviceConfig();
        }
        instance.checkAndReload();
        return instance;
    }

    private void checkAndReload() {
        if (configFile.exists()) {
            long modified = configFile.lastModified();
            if (modified > lastModified) {
                loadConfig();
            }
        }
    }

    private synchronized void loadConfig() {
        if (!configFile.exists()) {
            System.err.println("[DeviceConfig] 配置文件不存在: " + configFile.getAbsolutePath());
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            config = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            lastModified = configFile.lastModified();
            System.out.println("[DeviceConfig] 配置加载成功: " + config.size() + " 个用户/设备, 路径: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[DeviceConfig] 加载配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查用户是否存在
     */
    public boolean isUserExists(String userId) {
        return userId != null && config.containsKey(userId);
    }

    /**
     * 获取设备名称
     */
    public String getDeviceName(String userId) {
        return config.get(userId);
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin(String userId) {
        return "admin".equals(config.get(userId));
    }

    /**
     * 检查设备是否已注册（设备ID即用户ID）
     */
    public boolean isDeviceRegistered(String deviceId) {
        return deviceId != null && config.containsKey(deviceId);
    }

    /**
     * 获取用户可访问的设备ID列表
     * - 管理员返回所有设备
     * - 普通用户返回自己的设备ID
     */
    public List<String> getAccessibleDevices(String userId) {
        if (!isUserExists(userId)) {
            return Collections.emptyList();
        }
        if (isAdmin(userId)) {
            // 管理员返回所有非admin的设备
            List<String> devices = new ArrayList<>();
            for (Map.Entry<String, String> entry : config.entrySet()) {
                if (!"admin".equals(entry.getValue())) {
                    devices.add(entry.getKey());
                }
            }
            return devices;
        }
        // 普通用户只能访问自己的设备
        return Collections.singletonList(userId);
    }

    /**
     * 检查用户是否有权限访问指定设备
     */
    public boolean canAccessDevice(String userId, String deviceId) {
        if (!isUserExists(userId)) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        return userId.equals(deviceId);
    }
}
