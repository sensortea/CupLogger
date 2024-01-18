package com.sensortea.cuplogger;

import java.io.IOException;
import java.util.List;

public class ServerCore implements ServerCoreAPI {
    private final SerialPorts serialPorts;
    private final SerialConnectionsConfig connectionsConfig;
    private final TextFormatReader textFormatReader;

    public ServerCore(String baseDir) throws IOException {
        this.connectionsConfig = new SerialConnectionsConfig(baseDir);
        this.serialPorts = new SerialPorts(connectionsConfig, baseDir);
        this.textFormatReader = new TextFormatReader(baseDir);
    }

    @Override
    public Iterable<SerialConnectionInfo> listSerialConnections() {
        return serialPorts.listAndUpdateConnections();
    }

    @Override
    public void setSerialConnectionBaudRate(String serialNumber, int baudRate) throws IOException {
        connectionsConfig.setBaudRate(serialNumber, baudRate);
    }

    @Override
    public void setSerialConnectionDataCapture(String serialNumber, boolean dataCaptureOn) throws IOException {
        connectionsConfig.setDataCapture(serialNumber, dataCaptureOn);
    }

    @Override
    public void setSerialConnectionName(String serialNumber, String name) throws IOException {
        connectionsConfig.setName(serialNumber, name);
    }

    @Override
    public List<DataRecord> readEvents(String serialNumber, long startEpochMs, long endEpochMs) {
        return textFormatReader.readEvents(serialNumber, startEpochMs, endEpochMs);
    }
}
