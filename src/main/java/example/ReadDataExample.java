package example;

import com.sensortea.cuplogger.DataRecord;
import com.sensortea.cuplogger.TextFormatReader;

public class ReadDataExample {
    public static void main(String[] args) {
        String baseDir = "/tmp/cuplogger";
        String serialNumber = "242353135363516111A2";
        long startEpochMs = 1705318466234L;
        long endEpochMs = 1705418466234L;

        TextFormatReader reader = new TextFormatReader(baseDir);

        // 1. Find files
        System.out.println(reader.findDataFiles(serialNumber, startEpochMs, endEpochMs));

        // 2a. Parse individual lines
        DataRecord record = reader.parseEvent("1705318467715,Loop #0\n");

        // 2b. Scan records
        reader.scan(serialNumber, startEpochMs, endEpochMs, r -> {
            System.out.println(r.epochMs);
            return true;
        });
    }
}