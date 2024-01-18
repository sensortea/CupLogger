package com.sensortea.cuplogger;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class HttpEndpoint {
    private final ServerCoreAPI api;

    public HttpEndpoint(ServerCoreAPI api) {
        this.api = api;
    }

    public void start(int port) throws IOException {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException e) {
            System.out.println("ERROR: Could not start HttpEndpoint at " + port +
                    ": " + e.getMessage() +
                    ". Maybe you haven't stopped the server before starting it again? " +
                    "Or maybe this port is used by a different program?");
            System.exit(1);
            return;
        }
        server.createContext("/listSerialConnections", new ListSerialConnections());
        server.createContext("/setSerialConnectionName", new SetSerialConnectionName());
        server.createContext("/setSerialConnectionBaudRate", new SetSerialConnectionBaudRate());
        server.createContext("/setSerialConnectionDataCapture", new SetSerialConnectionDataCapture());
        // todo: JSON format for response here is not efficient at all...
        server.createContext("/getData", new GetData());
        server.createContext("/ui", new ResourceFilesHttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("SUCCESS: HttpEndpoint started at localhost:" + port);
    }

    class ListSerialConnections extends JsonPostHttpHandler {
        @Override
        void handle(CallContext ctx) {
            ctx.setResult(api.listSerialConnections());
        }
    }

    class SetSerialConnectionName extends JsonPostHttpHandler {
        @Override
        void handle(JsonPostHttpHandler.CallContext ctx) throws BadInputException, IOException {
            api.setSerialConnectionName(
                    ctx.getRequiredNonEmptyStringParam("serialNumber"),
                    ctx.getRequiredNonEmptyStringParam("name"));
        }
    }

    class SetSerialConnectionBaudRate extends JsonPostHttpHandler {
        @Override
        void handle(JsonPostHttpHandler.CallContext ctx) throws BadInputException, IOException {
            api.setSerialConnectionBaudRate(
                    ctx.getRequiredNonEmptyStringParam("serialNumber"),
                    ctx.getRequiredIntParam("baudRate"));
        }
    }

    class SetSerialConnectionDataCapture extends JsonPostHttpHandler {
        @Override
        void handle(JsonPostHttpHandler.CallContext ctx) throws BadInputException, IOException {
            api.setSerialConnectionDataCapture(
                    ctx.getRequiredNonEmptyStringParam("serialNumber"),
                    ctx.getRequiredNonEmptyBooleanParam("dataCaptureOn"));
        }
    }

    class GetData extends JsonPostHttpHandler {
        @Override
        void handle(JsonPostHttpHandler.CallContext ctx) throws BadInputException {
            ctx.setResult(
                    api.readEvents(
                            ctx.getRequiredNonEmptyStringParam("serialNumber"),
                            ctx.getRequiredLongParam("startEpochMs"),
                            ctx.getRequiredLongParam("endEpochMs")));
        }
    }
}
