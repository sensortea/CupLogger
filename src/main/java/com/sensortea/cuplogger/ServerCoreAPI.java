package com.sensortea.cuplogger;

import java.io.IOException;
import java.util.List;

// todo: do we need this interface? or should we just have implementation, since the implementation is mostly delegating
public interface ServerCoreAPI {
    /* Data capture */
    /**
     * @return list of known serial connections, past (unless removed) or current
     */
    Iterable<SerialConnectionInfo> listSerialConnections();

    /**
     * Sets baud rate
     * @param serialNumber serial port SN
     * @param baudRate baud rate
     */
    void setSerialConnectionBaudRate(String serialNumber, int baudRate) throws IOException;

    /**
     * Sets data capture on or off
     * @param serialNumber serial port SN
     * @param dataCaptureOn true to set on, false to off
     */
    void setSerialConnectionDataCapture(String serialNumber, boolean dataCaptureOn) throws IOException;

    /**
     * Sets user-defined name
     * @param serialNumber serial port SN
     * @param name name to use
     */
    void setSerialConnectionName(String serialNumber, String name) throws IOException;

    /* Data retrieval */
    /**
     * Reads all events of a given connection for a given time range.
     * @param serialNumber of the connection
     * @param startEpochMs start of the interval
     * @param endEpochMs end of the interval
     * @return all events
     */
    List<DataRecord> readEvents(String serialNumber, long startEpochMs, long endEpochMs);
}
