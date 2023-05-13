package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class RegistrationHandler implements HttpHandler {

    private UserAuthenticator auth;

    public RegistrationHandler(UserAuthenticator newAuth) {
        auth = newAuth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Headers headers = exchange.getRequestHeaders();
        String contentType = "";

        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    sendResponse(exchange, 400, "Content-type wasn't available");
                    return;
                }
                if (contentType.equalsIgnoreCase("application/json")) {
                    InputStream stream = exchange.getRequestBody();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    String requestBody = rd.lines().collect(Collectors.joining());
                    JSONObject jsonObj = new JSONObject(requestBody);
                    String username = jsonObj.getString("username");
                    String password = jsonObj.getString("password");
                    String email = jsonObj.optString("email");

                    if (username.isBlank() || password.isBlank()) {
                        sendResponse(exchange, 400, "Username or password was blank");
                        return;
                    }
                    if (!auth.addUser(username, password, email)) {
                        sendResponse(exchange, 403, "Can't register the same user twice");
                        return;
                    }
                    exchange.sendResponseHeaders(200, -1);
                    stream.close();
                } else {
                    sendResponse(exchange, 400, "Content-Type wasn't application/json");
                }
            } else {
                sendResponse(exchange, 407, "Only POST is accepted");
                return;
            }

        } catch (JSONException je) {
            sendResponse(exchange, 400, "Invalid JSON format");
            return;
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal server error");
        }

    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
                out.flush();
            } catch (Exception e) {
            } finally {
                exchange.close();
            }
        } catch (IOException ioe) {
        }
    }
}