package com.sensortea.cuplogger;

import java.io.IOException;
import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expecting <base_dir> <port>. Got only: " + Arrays.toString(args));
            System.exit(1);
            return;
        }
        String baseDir = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Expecting <base_dir> <port>. Couldn't parse port argument: " + args[1]);
            System.exit(1);
            return;
        }
        try {
            FileCheckUtil.ensureDirectoryExistsAndUsable(baseDir);
            ServerCoreAPI coreApi = new ServerCore(baseDir);
            HttpEndpoint httpEndpoint = new HttpEndpoint(coreApi);
            httpEndpoint.start(port);
            System.out.println("CupLogger server started. Open browser at http://localhost:" + port +
                    "/ui/index.html to access the web UI.");
        } catch (IOException e) {
            System.out.println("Could not start up due to '" + e.getMessage() + "'. Exiting.");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
