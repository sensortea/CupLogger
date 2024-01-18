package com.sensortea.cuplogger;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialPorts {
    private static final Logger LOG = Logger.getLogger(SerialPorts.class.getName());

    private final SerialConnectionsConfig connectionsConfig;
    private final String baseDir;
    private final ConcurrentHashMap<String, RunningTask> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static class RunningTask {
        final Future<?> future;
        final int baudRate;

        public RunningTask(Future<?> future, int baudRate) {
            this.future = future;
            this.baudRate = baudRate;
        }
    }

    public SerialPorts(SerialConnectionsConfig connectionsConfig, String baseDir) throws IOException {
        this.connectionsConfig = connectionsConfig;
        this.baseDir = baseDir;
        connectionsConfig.addChangeCallback(this::syncAll);
        // Make sure reader tasks are running as per configuration, and as per available ports, incl. if tasks exited.
        syncAll(connectionsConfig.list());
        syncAllPeriodically();
    }

    /**
     * Reads configured ports, updates their details with live info and adds newly discovered live ports.
     *
     * @return all known ports
     */
    public Iterable<SerialConnectionInfo> listAndUpdateConnections() {
        Map<String, SerialConnectionInfo> configured;
        try {
            configured = connectionsConfig.list();
        } catch (IOException e) {
            // todo: think through it; move to config class?
            LOG.log(Level.WARNING, "Failed to read config", e);
            configured = new HashMap<>();
        }

        boolean updateConfig = false;
        for (SerialPort livePort : listUsbPorts()) {
            String serialNumber = getSerialNumber(livePort);
            // todo: add newly discovered periodically in sync method instead of here?
            if (!configured.containsKey(serialNumber)) {
                SerialConnectionInfo item = new SerialConnectionInfo();
                item.name = "New Device";
                item.serialNumber = serialNumber;
                item.vendorID = livePort.getVendorID();
                item.productID = livePort.getProductID();
                item.descriptivePortName = livePort.getDescriptivePortName();
                item.systemPortName = livePort.getSystemPortName();
                item.systemPortPath = livePort.getSystemPortPath();
                item.portDescription = livePort.getPortDescription();
                item.portLocation = livePort.getPortLocation();
                item.baudRate = livePort.getBaudRate();
                item.connected = true;
                item.dataCaptureOn = false;
                configured.put(item.serialNumber, item);
                LOG.info("Added new connection to configuration with serial number: " + item.serialNumber);
                updateConfig = true;
            } else {
                SerialConnectionInfo serialConnectionInfo = configured.get(serialNumber);
                serialConnectionInfo.portLocation = livePort.getPortLocation();
                // we need another attribute: reading task active or not
                serialConnectionInfo.connected = true;
            }
        }
        if (updateConfig) {
            try {
                connectionsConfig.update(configured);
            } catch (IOException e) {
                // todo: think through it; move to config class?
                LOG.log(Level.WARNING, "Failed to save config", e);
            }
        }

        List<SerialConnectionInfo> result = new ArrayList<>(configured.values());
        result.sort(Comparator.comparing(o -> o.name));
        return result;
    }

    public static String getSerialNumber(SerialPort port) {
        return port.getSerialNumber().replaceAll("[^A-Za-z0-9]", "_");
    }

    static Iterable<SerialPort> listUsbPorts() {
        List<SerialPort> result = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            if (!"Unknown".equals(getSerialNumber(p))) {
                result.add(p);
            }
        }
        return result;
    }

    private void syncAllPeriodically() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Map<String, SerialConnectionInfo> configured;
                try {
                    configured = connectionsConfig.list();
                } catch (IOException e) {
                    // we'll retry
                    LOG.warning("Was not able to read from connections config, " +
                            "will not be able to read from re-connected connections.");
                    return;
                }
                syncAll(configured);
            }
        }, 0, 2000);
    }

    private void syncAll(Map<String, SerialConnectionInfo> configured) {
        // 0. remove completed tasks
        Set<String> completedTaskKeys = new HashSet<>();
        for (Map.Entry<String, RunningTask> entry : runningTasks.entrySet()) {
            if (entry.getValue().future.isDone()) {
                completedTaskKeys.add(entry.getKey());
            }
        }
        for (String key : completedTaskKeys) {
            runningTasks.remove(key);
        }

        // 1. stop those that were removed from configured or were configured to data capture off
        // 1b. restart those that have new baud rate
        for (Map.Entry<String, RunningTask> taskEntry : runningTasks.entrySet()) {
            String serialNumber = taskEntry.getKey();
            if (!configured.containsKey(serialNumber)) {
                LOG.info("Stopping listening to " + serialNumber + " as it is no longer found in config.");
                stopListening(serialNumber);
            } else {
                SerialConnectionInfo config = configured.get(serialNumber);
                if (!config.dataCaptureOn) {
                    LOG.info("Stopping listening to " + serialNumber + " as its dataCaptureOn now false.");
                    stopListening(serialNumber);
                } else {
                    // 2b. restart those that have new baud rate
                    if (taskEntry.getValue().baudRate != config.baudRate) {
                        LOG.info("Restarting listening to " + serialNumber +
                                " due to baud rate change: " + taskEntry.getValue().baudRate + "->" + config.baudRate);
                        stopListening(serialNumber);
                        // todo: should we wait for task to end nicely?
                        startListening(serialNumber, config.baudRate);
                    }
                }
            }
        }

        // 2. start based on configuration (those that are available)
        for (SerialPort availablePort : listUsbPorts()) {
            String serialNumber = getSerialNumber(availablePort);
            if (configured.containsKey(serialNumber)) {
                SerialConnectionInfo config = configured.get(serialNumber);
                if (!runningTasks.containsKey(serialNumber)) {
                    if (config.dataCaptureOn) {
                        startListening(config.serialNumber, config.baudRate);
                    }
                }
            }
        }
    }

    private void startListening(String serialNumber, int baudRate) {
        SerialReaderTask task = new SerialReaderTask(serialNumber, baudRate, baseDir);
        Future<?> future = executorService.submit(task);
        runningTasks.put(serialNumber, new RunningTask(future, baudRate));
    }

    private void stopListening(String serialNumber) {
        RunningTask task = runningTasks.remove(serialNumber);
        if (task != null) {
            task.future.cancel(true);
        } else {
            LOG.warning("Couldn't stop port listening task: task not found for port: " + serialNumber);
        }
    }
}