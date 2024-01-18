package com.sensortea.cuplogger;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class SerialReaderTask implements Runnable {
    private static final Logger LOG = Logger.getLogger(SerialReaderTask.class.getName());
    private final String serialNumber;
    private final int baudRate;
    private SerialPort serialPort;
    private final String baseDir;
    byte[] readBuffer = new byte[32 * 1024];

    public SerialReaderTask(String serialNumber, int baudRate, String baseDir) {
        this.serialNumber = serialNumber;
        this.baudRate = baudRate;
        this.baseDir = baseDir;
    }

    @Override
    public void run() {
        TextFormatWriter writer = null;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (serialPort == null) {
                    if (!connectToSerialPort()) {
                        // the task manager will have to re-run the task
                        break;
                    }
                    writer = new TextFormatWriter(baseDir, serialNumber);
                }
                if (!serialPort.isOpen()) {
                    LOG.warning("Won't read from not opened port " + serialNumber + ". Will stop now.");
                    break;
                }
                int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                if (numRead > 0) {
                    writer.write(readBuffer, numRead);
                } else if (numRead == 0) {
                    Thread.sleep(20);
                } else { // numRead is -1
                    // todo: this is poor man's detection.. but somehow it's tough to do anything else
                    LOG.warning("Had issues reading from the port " + serialNumber +
                            ". Will stop reading from it now (reading should resume automatically if the port is good).");
                    break;
                    // todo: track the quality of connection with reconnect stats?
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info("Thread interrupted: " + e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.INFO, "Error in serial communication with " + serialNumber + " on " + serialPort, e);
        } finally {
            if (serialPort != null) {
                try {
                    serialPort.closePort();
                } catch (Exception e) {
                    // nothing to do: parent will restart the task if needed and will retry to open
                    LOG.log(Level.INFO, "Error while closing port of " + serialNumber + ": " + serialPort, e);
                }
            }
            LOG.info("Stopped listening on " + serialNumber + ", baud rate: " + baudRate + " sn: " + serialNumber);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // nothing to do really here
                    LOG.log(Level.INFO, "Failed to close writer " + writer, e);
                }
            }
        }
    }

    private boolean connectToSerialPort() throws InterruptedException {
        for (SerialPort p : SerialPorts.listUsbPorts()) {
            if (serialNumber.equals(SerialPorts.getSerialNumber(p))) {
                serialPort = p;
                break;
            }
        }
        if (serialPort == null) {
            LOG.warning("Couldn't find the port " + serialNumber + " among available ports.");
            return false;
        }
        serialPort.setBaudRate(baudRate);
        if (!serialPort.openPort()) {
            LOG.info("Failed to open port: " + serialNumber +
                    // somehow saw in tests with bad baud rate it fails here
                    ". Maybe baud rate " + baudRate + " is not good?");
            try {
                // Just in case try to release all resources. Likely this OCD is not needed
                serialPort.closePort();
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to close port " + serialPort + " sn: " + serialNumber, e);
            }
            return false;
        }
        LOG.info("Started listening on " + serialPort + ", baud rate: " + baudRate + " sn: " + serialNumber);
        return true;
    }
}
