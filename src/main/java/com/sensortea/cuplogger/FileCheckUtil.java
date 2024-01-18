package com.sensortea.cuplogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

public class FileCheckUtil {
    private static final Logger LOG = Logger.getLogger(SerialPorts.class.getName());
    public static void ensureDirectoryExistsAndUsable(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            LOG.info("Directory " + dir.getPath() + " doesn't exist. Will create it now.");
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + dir.getPath());
            }
        } else {
            if (!dir.isDirectory()) {
                throw new IOException("Path is not a directory: " + dir.getPath());
            }
            if (!dir.canExecute()) {
                throw new IOException("Directory cannot be opened: " + dir.getPath());
            }
            if (!dir.canRead()) {
                throw new IOException("Directory cannot be read: " + dir.getPath());
            }
            if (!dir.canWrite()) {
                throw new IOException("Directory cannot be written to: " + dir.getPath());
            }
        }
    }

    public static void ensureFileExistsAndUsable(String filePath, byte[] newFileContents) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) {
            try {
                LOG.info("File " + f.getPath() + " doesn't exist. Will create it now.");
                Files.write(f.toPath(), newFileContents, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                throw new IOException("Failed to create file: " + f.getPath(), e);
            }
        } else {
            if (f.isDirectory()) {
                throw new IOException("Expected a file, but it is a directory: " + f.getPath());
            }
            if (!f.canRead()) {
                throw new IOException("File cannot be read: " + f.getPath());
            }
            if (!f.canWrite()) {
                throw new IOException("File cannot be written to: " + f.getPath());
            }
        }
    }

}
