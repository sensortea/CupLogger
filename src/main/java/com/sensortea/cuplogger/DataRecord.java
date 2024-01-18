package com.sensortea.cuplogger;

public class DataRecord {
    public long epochMs;
    // null if could parse event (parsedEvent is not null)
    public String rawText;
    public Event parsedEvent;

    public static class Event {
        public String programId;
        public String programVersion;
        public String deviceConfig;
        public long timeDelta;
        public String logMessage;
        public String[] readingIds;
        public double[] readingValues;
    }
}