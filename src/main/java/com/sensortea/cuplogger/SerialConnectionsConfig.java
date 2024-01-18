package com.sensortea.cuplogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

// todo: lock file when modifying?
public class SerialConnectionsConfig {
    private final String filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Collection<Consumer<Map<String, SerialConnectionInfo>>> changeCallbacks = new ArrayList<>();

    public SerialConnectionsConfig(String baseDir) throws IOException {
        filePath = baseDir + File.separator + "serialConnections.json";
        FileCheckUtil.ensureFileExistsAndUsable(
                filePath, gson.toJson(Collections.EMPTY_MAP).getBytes(StandardCharsets.UTF_8));
    }

    public synchronized void addChangeCallback(Consumer<Map<String, SerialConnectionInfo>> callback) {
        changeCallbacks.add(callback);
    }

    public synchronized void update(Map<String, SerialConnectionInfo> list) throws IOException {
        saveToFile(list);
    }

    public synchronized Map<String, SerialConnectionInfo> list() throws IOException {
        // todo: make a copy if we decide to cache
        return loadFromFile();
    }

    public synchronized void setBaudRate(String serialNumber, int baudRate) throws IOException {
        update(serialNumber, portInfo -> portInfo.baudRate = baudRate);
    }

    public synchronized void setDataCapture(String serialNumber, boolean dataCaptureOn) throws IOException {
        update(serialNumber, portInfo -> portInfo.dataCaptureOn = dataCaptureOn);
    }

    public synchronized void setName(String serialNumber, String name) throws IOException {
        update(serialNumber, portInfo -> portInfo.name = name);
    }

    private synchronized Map<String, SerialConnectionInfo> loadFromFile() throws IOException {
        // todo: cache based on the file modified timestamp
        try (Reader reader = new FileReader(filePath)) {
            // todo: here and everywhere: handle bad config file (e.g. manually corrupted) - bad json
            Map<String, SerialConnectionInfo> serialPortInfos =
                    gson.fromJson(reader, new TypeToken<HashMap<String, SerialConnectionInfo>>() {}.getType());
            if (serialPortInfos == null) {
                serialPortInfos = new HashMap<>();
            }
            // clear up live connection state
            serialPortInfos.values().forEach(serialConnectionInfo -> {
                serialConnectionInfo.portLocation = null;
                serialConnectionInfo.connected = false;
            });
            return serialPortInfos;
        }
    }

    private synchronized void saveToFile(Map<String, SerialConnectionInfo> serialPortInfos) throws IOException {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(serialPortInfos, writer);
        }
        changeCallbacks.forEach(consumer -> consumer.accept(serialPortInfos));
    }

    private synchronized void update(String serialNumber, Consumer<SerialConnectionInfo> updateFunction) throws IOException {
        Map<String, SerialConnectionInfo> map = loadFromFile();
        if (!map.containsKey(serialNumber)) {
            // todo: think and throw smth
            return;
        }
        updateFunction.accept(map.get(serialNumber));
        saveToFile(map);
    }
}