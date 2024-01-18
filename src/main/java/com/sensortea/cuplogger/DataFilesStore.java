package com.sensortea.cuplogger;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/** Data store structure:
data
 └── yyyy_MM_dd
     └── serialNumber
          ├── yyyy_MM_dd_HHmmss.format
          └── yyyy_MM_dd_HHmmss.format

Each file can hold maximum FILE_INTERVAL_MS millis of data

E.g.:
data
 └── 2023_12_10
 │    └── 242353135363516111A2
 │        └── 2023_12_10_171145.txt
 └── 2023_12_11
      └── 242353135363516111A2
          └── 2023_12_11_095840.txt

*/
public class DataFilesStore {
    public static final String DATA_DIR = "data";
    // NOTE: doesn't mean there's one file per interval; also doesn't mean filename time starts at interval start
    public static final int FILE_INTERVAL_MS = 1000 * 60 * 10;
    private static final String FILE_PREFIX_DATE_FORMAT = "yyyy_MM_dd_HHmmss";
    private static final String DIR_DATE_FORMAT = "yyyy_MM_dd";

    public static File newDataFile(long createTimeEpochMs, String format,
                                   String baseDir, String serialNumber) throws IOException {
        Date date = new Date(createTimeEpochMs);
        SimpleDateFormat df = getDirDateFormat();
        String dateFolder = df.format(date);
        String filePath = getDataDirPath(baseDir) + File.separator + dateFolder + File.separator + serialNumber;
        FileCheckUtil.ensureDirectoryExistsAndUsable(filePath);
        File directory = new File(filePath);
        String timestamp = getFilePrefixDateFormat().format(date);
        return new File(directory, timestamp + "." + format);
    }

    public static Iterable<File> findDataFiles(String baseDir, String serialNumber,
                                               String fileFormat, long startEpochMs, long endEpochMs) {

        long fileStartEpochMs = (startEpochMs / DataFilesStore.FILE_INTERVAL_MS) * DataFilesStore.FILE_INTERVAL_MS;
        long fileEndEpochMs = (endEpochMs / DataFilesStore.FILE_INTERVAL_MS) * DataFilesStore.FILE_INTERVAL_MS
                                // file can contain additional DataStoreFormat.FILE_INTERVAL_MS - 1 range of points
                                + DataFilesStore.FILE_INTERVAL_MS - 1;

        SimpleDateFormat dirFmt = getDirDateFormat();
        String startDir = dirFmt.format(new Date(fileStartEpochMs));
        String endDir = dirFmt.format(new Date(fileEndEpochMs));
        SimpleDateFormat fileFmt = getFilePrefixDateFormat();
        String startFile = fileFmt.format(new Date(fileStartEpochMs));
        String endFile = fileFmt.format(new Date(fileEndEpochMs));

        List<File> result = new ArrayList<>();
        File dataDir = DataFilesStore.getDataDir(baseDir);
        File[] directories = dataDir.listFiles(File::isDirectory);
        if (directories != null) {
            Arrays.sort(directories, Comparator.comparing(File::getName));
            for (File directory : directories) {
                String dirName = directory.getName();
                // Filter directories based on the date range
                if (dirName.compareTo(startDir) >= 0 && dirName.compareTo(endDir) <= 0) {
                    File serialDir = new File(directory, serialNumber);
                    if (serialDir.exists()) {
                        File[] files = serialDir.listFiles((dir, name) -> name.endsWith(fileFormat));
                        if (files != null) {
                            Arrays.sort(files, Comparator.comparing(File::getName));
                            for (File file : files) {
                                String fileName = file.getName().split("\\.")[0];
                                // Use string comparison to check if the file's date and time are within the range
                                if (fileName.compareTo(startFile) >= 0 && fileName.compareTo(endFile) <= 0) {
                                    result.add(file);
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static SimpleDateFormat getDirDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat(DIR_DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    private static long parseDirEpochMs(String dirName, SimpleDateFormat fmt) throws ParseException {
        return fmt.parse(dirName).getTime();
    }

    private static SimpleDateFormat getFilePrefixDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat(FILE_PREFIX_DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    private static long parseFileEpochMs(String fileName, SimpleDateFormat fmt) throws ParseException {
        return fmt.parse(fileName.substring(0, FILE_PREFIX_DATE_FORMAT.length())).getTime();
    }

    private static String getDataDirPath(String baseDir) {
        return baseDir + File.separator + DATA_DIR;
    }

    private static File getDataDir(String baseDir) {
        return new File(getDataDirPath(baseDir));
    }
}
