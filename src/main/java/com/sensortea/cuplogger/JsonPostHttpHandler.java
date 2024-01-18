package com.sensortea.cuplogger;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of {@link HttpHandler} that: <br/>
 * 1. expects only POST requests <br/>
 * 2. expects input data (parameters) to be passed in body in JSON format <br/>
 * 3. responds with result in JSON format <br/>
 * Uses {@link Gson} for JSON serde, which is the only dependency apart from standard JDK.
 */
abstract public class JsonPostHttpHandler implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(JsonPostHttpHandler.class.getName());
    private final Gson gson = new Gson();

    abstract void handle(CallContext ctx) throws BadInputException, IOException;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (sendErrorResponseIfNotPost(exchange)) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            Map<String, String> input;
            try {
                input = gson.fromJson(reader, new TypeToken<Map<String, String>>() {
                }.getType());
            } catch (JsonParseException e) {
                sendErrorResponse(exchange, "Bad json body provided. " + e.getMessage(), 400);
                return;
            }

            try {
                CallContext ctx = new CallContext(input);
                handle(ctx);
                sendResponse(exchange, ctx.result);
            } catch (BadInputException e) {
                sendErrorResponse(exchange, e.getMessage(), 400);
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, "Error processing request: " + e.getMessage(), 500);
            LOG.log(Level.WARNING, "Error processing request", e);
        }
    }

    private void sendResponse(HttpExchange exchange, Object result) throws IOException {
        String response = gson.toJson(result);
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    protected void sendErrorResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static boolean sendErrorResponseIfNotPost(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String response = "Method not allowed";
            exchange.sendResponseHeaders(405, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return true;
        }
        return false;
    }

    protected static final class CallContext {
        private final Map<String, String> input;
        private Object result = new Object();

        private CallContext(Map<String, String> input) {
            this.input = input;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public String getRequiredNonEmptyStringParam(String key) throws BadInputException {
            String value = input.get(key);
            if (null == value || value.isEmpty()) {
                throw new BadInputException("Non-empty '" + key + "' must be specified.");
            }
            return value;
        }

        public boolean getRequiredNonEmptyBooleanParam(String key) throws BadInputException {
            String value = input.get(key);
            if (null == value || value.isEmpty()) {
                throw new BadInputException("Non-empty '" + key + "' must be specified.");
            }

            return Boolean.parseBoolean(value);
        }

        public int getRequiredIntParam(String key) throws BadInputException {
            String value = input.get(key);
            if (null == value || value.isEmpty()) {
                throw new BadInputException("Non-empty '" + key + "' must be specified.");
            }

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BadInputException("'" + value + "' is not valid int value for '" + key + "'");
            }
        }

        public long getRequiredLongParam(String key) throws BadInputException {
            String value = input.get(key);
            if (null == value || value.isEmpty()) {
                throw new BadInputException("Non-empty '" + key + "' must be specified.");
            }

            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new BadInputException("'" + value + "' is not valid long value for '" + key + "'");
            }
        }
    }

    protected static class BadInputException extends Exception {
        public BadInputException(String message) {
            super(message);
        }
    }
}
