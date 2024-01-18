package com.sensortea.cuplogger;

import java.io.*;

// todo: support compression?
public class TextFormatWriter implements Closeable {
    public static final String FORMAT = "txt";
    private final String baseDir;
    private final String serialNumber;
    private long curFileIntervalStartEpochMs = 0;
    private OutputStream outputStream;

    private boolean newLineStart = true;

    // NOTE: the file can exceed maxFileSize as we only split at the line boundaries
    public TextFormatWriter(String baseDir, String serialNumber) {
        this.baseDir = baseDir;
        this.serialNumber = serialNumber;
    }

    public void write(byte[] data, int length) throws IOException {
        long epochMs = System.currentTimeMillis();
        if (outputStream == null) {
            createNewFile(epochMs);
        } else if (shouldCreateNewFile(epochMs)) {
            createNewFile(epochMs);
        }
        // Need to insert timestamp at the beginning of each line, BUT
        // need to use timestamp of when we received the line. So can't just insert current timestamp after \n, need
        // to wait to receive the line.
        // Re-using the timestamp for all data received, to avoid suggesting that multiple lines in given data
        // were produced at different time.
        String timestamp = epochMs + ",";
        for (int i = 0; i < length; i++) {
            // Check for newline character
            if (data[i] == '\n') {
                // Write the newline character
                outputStream.write(data[i]);
                newLineStart = true;
            } else {
                if (newLineStart) {
                    // Write the timestamp after the newline
                    outputStream.write(timestamp.getBytes());
                    newLineStart = false;
                }
                // Write the current byte
                outputStream.write(data[i]);
            }
        }
        outputStream.flush();
    }

    @Override
    public String toString() {
        return "TextFormatWriter{" + "baseDir='" + baseDir + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", curFileIntervalStartEpochMs=" + curFileIntervalStartEpochMs +
                '}';
    }

    private boolean shouldCreateNewFile(long epochMs) {
        // todo: leaking format, not nice
        return epochMs - curFileIntervalStartEpochMs > DataFilesStore.FILE_INTERVAL_MS;
    }

    private void createNewFile(long epochMs) throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }

        File file = DataFilesStore.newDataFile(epochMs, FORMAT, baseDir, serialNumber);
        curFileIntervalStartEpochMs = (epochMs / DataFilesStore.FILE_INTERVAL_MS) * DataFilesStore.FILE_INTERVAL_MS;

        outputStream = new BufferedOutputStream(new FileOutputStream(file, true));
    }

    @Override
    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
    }
}
