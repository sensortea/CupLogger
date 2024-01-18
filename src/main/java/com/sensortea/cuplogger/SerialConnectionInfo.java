package com.sensortea.cuplogger;

public final class SerialConnectionInfo {
    // Unique identification
    // This is really an ID, vendorID and productID are not supposed to change
    String serialNumber;
    int vendorID;
    int productID;

    // These may change, as they tell where the device was connected last
    String descriptivePortName;
    String systemPortName;
    String systemPortPath;
    String portDescription;
    String portLocation;
    boolean connected; // todo: do we need this if we have portLocation?

    // These are user settings, can be changed by users
    int baudRate;
    boolean dataCaptureOn;
    String name;
}
