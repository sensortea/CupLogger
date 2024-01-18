package com.sensortea.cuplogger;

import java.io.*;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextFormatReader {
    private static final Logger LOG = Logger.getLogger(TextFormatReader.class.getName());
    private final String baseDir;

    public TextFormatReader(String baseDir) {
        this.baseDir = baseDir;
    }

    public Iterable<File> findDataFiles(String serialNumber, long startEpochMs, long endEpochMs) {
        return DataFilesStore.findDataFiles(baseDir, serialNumber, TextFormatWriter.FORMAT, startEpochMs, endEpochMs);
    }

    public List<DataRecord> readEvents(String serialNumber, long startEpochMs, long endEpochMs) {
        List<DataRecord> events = new ArrayList<>(4 * 1024);
        scan(serialNumber, startEpochMs, endEpochMs, r -> {
            events.add(r);
            return true;
        });
        return events;
    }

    public interface RecordConsumer {
        // stops scan when returns false
        boolean acceptAndContinue(DataRecord r);
    }

    public void scan(String serialNumber, long startEpochMs, long endEpochMs, RecordConsumer consumer) {
        long startMs = System.currentTimeMillis();

        int eventsScanned = 0;
        for (File f : DataFilesStore.findDataFiles(baseDir, serialNumber, TextFormatWriter.FORMAT,
                                                    startEpochMs, endEpochMs)) {
            eventsScanned += scanFile(startEpochMs, endEpochMs, consumer, f);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        LOG.fine("Scanned: " + eventsScanned + " in " + elapsedMs + "ms. Events per sec: " +
                String.format("%,2d", ((long) eventsScanned * 1000 / (elapsedMs == 0 ? 1 : elapsedMs))));
    }

    private static int scanFile(long startEpochMs, long endEpochMs, RecordConsumer consumer, File file) {
        int eventsScanned = 0;
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis,
                     StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE));
             BufferedReader br = new BufferedReader(isr)) {

            Iterator<String> lineIterator = br.lines().iterator();

            while (lineIterator.hasNext()) {
                String line = lineIterator.next();
                DataRecord e = parseEvent(line);
                eventsScanned++;
                if (e != null) {
                    if (e.epochMs >= startEpochMs && e.epochMs < endEpochMs) {
                        if (!consumer.acceptAndContinue(e)) {
                            break; // Break out of the loop when the condition is met
                        }
                    }
                }
            }
        } catch (IOException e) {
            // this shouldn't really happen.. todo: maybe also check dir for correct access/etc.
            // just skipping the file; todo: maybe propagate to user somehow?
            LOG.log(Level.INFO, "Error reading file: " + file.getAbsolutePath() + ", skipping.", e);
        }
        return eventsScanned;
    }

    /*
        - text format: <epochMs>,<programId>,<programVersion>,<deviceConfig>,
                       <timeDelta>,<logMessage>[,<readingName>,<readingValue>[,<readingName>,<readingValue>[...]]
     */
    public static DataRecord parseEvent(String line) {
        // todo: efficiency can be greatly improved
        //       incl. by checking number of parts and failing parse fast
        try {
            String[] parts = line.split(",");
            DataRecord de = new DataRecord();
            de.epochMs = Long.parseLong(parts[0]);
            try {
                DataRecord.Event e = new DataRecord.Event();
                e.programId = parts[1];
                e.programVersion = parts[2];
                e.deviceConfig = parts[3];
                e.timeDelta = parts[4].isEmpty() ? 0 : Long.parseLong(parts[4]);
                e.logMessage = parts[5];
                e.readingIds = new String[parts.length - 6 - 1];
                e.readingValues = new double[parts.length - 6 - 1];
                for (int i = 6; i < parts.length - 1; i++) {
                    String[] readingParts = parts[i].split(":");
                    e.readingIds[i - 6] = readingParts[0];
                    e.readingValues[i - 6] = Double.parseDouble(readingParts[1]);
                }
                // length check
                int lengthCheck = Integer.parseInt(parts[parts.length - 1]);
                int firstCommaIdx = line.indexOf(",");
                int lastCommaIdx = line.lastIndexOf(",");
                if (lastCommaIdx - firstCommaIdx - 1 != lengthCheck) {
                    de.rawText = line.substring(parts[0].length() + 1);
                    return de;
                }
                de.parsedEvent = e;
            } catch (Exception e) {
                de.rawText = line.substring(parts[0].length() + 1);
                return de;
            }
            return de;
        } catch (Exception e) {
            // can only happen if writing to file was corrupted..
            return null;
        }
    }
}
