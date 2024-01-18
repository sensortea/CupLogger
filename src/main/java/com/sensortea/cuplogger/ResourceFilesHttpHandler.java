package com.sensortea.cuplogger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceFilesHttpHandler implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(ResourceFilesHttpHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        serveResource(exchange, exchange.getRequestURI().getPath());
    }

    private void serveResource(HttpExchange exchange, String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                }
            }
        } catch (Exception e) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            LOG.log(Level.WARNING, "Error processing request", e);
        } finally {
            exchange.close();
        }
    }
}
