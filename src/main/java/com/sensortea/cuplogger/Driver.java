package com.sensortea.cuplogger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.*;

public class Driver {
    public static final String USAGE = "Usage:\n" +
            "  java -jar <program.jar> start <baseDir> <port>\n" +
            "  java -jar <program.jar> status <baseDir>\n" +
            "  java -jar <program.jar> start <baseDir>\n";
    public static final String PROGRAM_LIVE_INFO_FILE = "program.live-info";

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Invalid arguments. " + USAGE);
        }

        String command = args[0];
        String baseDir = args[1];
        FileCheckUtil.ensureDirectoryExistsAndUsable(baseDir);

        configureLogger(baseDir);

        switch (command) {
            case "start":
                ServerMain.main(Arrays.copyOfRange(args, 1, args.length));
                startApplication(baseDir);
                break;
            case "status":
                checkStatus(baseDir);
                break;
            case "stop":
                stopApplication(baseDir);
                break;
            default:
                System.out.println("Invalid command. " + USAGE);
                System.exit(1);
        }
    }

    private static void configureLogger(String baseDir) {
        Logger rootLogger = Logger.getLogger("");
        // Remove existing handlers, including the default console handler
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        String logLevelName = System.getenv("LOG_LEVEL");
        Level logLevel = logLevelName != null ? Level.parse(logLevelName) : Level.INFO;
        rootLogger.setLevel(logLevel);

        String logFilePath = Paths.get(baseDir, "application.log").toString();
        try {
            FileHandler fileHandler = new FileHandler(logFilePath, true);
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Could not set up file handler for logger: " + e.getMessage());
        }
    }

    private static void startApplication(String baseDir) {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            File lockFile = new File(baseDir, PROGRAM_LIVE_INFO_FILE);
            if (lockFile.exists()) {
                System.out.println("Program might already be running, will try to stop it first.");
                stopApplication(baseDir);
            }
            if (lockFile.exists()) {
                System.out.println("Program might be already running and couldn't stop it. Remove " +
                        lockFile + " _if you are sure_ it is not, and try again.");
                return;
            }
            Files.write(lockFile.toPath(), ("" + port).getBytes());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (lockFile.exists()) {
                    lockFile.delete();
                }
            }));
            listenForStopCommand(serverSocket, lockFile);

        } catch (IOException e) {
            System.out.println("Could not start program due to: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void checkStatus(String baseDir) {
        File lockFile = new File(baseDir, PROGRAM_LIVE_INFO_FILE);
        if (lockFile.exists()) {
            System.out.println("Application is running");
        } else {
            System.out.println("Application is not running");
        }
    }

    private static void stopApplication(String baseDir) {
        File lockFile = new File(baseDir, PROGRAM_LIVE_INFO_FILE);
        if (!lockFile.exists()) {
            System.out.println("Application is not running");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(new String(Files.readAllBytes(lockFile.toPath())));
        } catch (Exception e) {
            System.out.println("Failed to read live info of the running program: " + e.getMessage() +
                    ", please retry. If program persists, try to force stop the program, and then remove " + lockFile +
                    " manually.");
            return;
        }
        try (Socket socket = new Socket("localhost", port)) {
            // Establishing a connection sends a stop signal to the server socket.
            System.out.println("Stopping the application");
        } catch (IOException e) {
            System.out.println("Failed to stop running program: " + e.getMessage() +
                    ", please retry. If program persists, try to force stop the program, and then remove " + lockFile +
                    " manually.");
        }
    }

    private static void listenForStopCommand(ServerSocket serverSocket, File lockFile) {
        try {
            serverSocket.accept();
            System.out.println("Stop command received");
            serverSocket.close();
            if (!lockFile.delete()) {
                System.out.println("Failed to delete " + lockFile +
                        ". Delete it manually before starting the program again");
            }
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Error listening for stop command: " + e.getMessage());
        }
    }
}
