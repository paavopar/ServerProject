package com.server;

import com.sun.net.httpserver.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WarningsHandler implements HttpHandler {

    private static MessageDatabase db;
    private static final String DBNAME = "Database";

    public WarningsHandler() {
        db = MessageDatabase.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Implementation of POST
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {

            Headers reqHeaders = exchange.getRequestHeaders();
            String contentType = reqHeaders.getFirst("Content-Type");
            if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
                sendResponse(exchange, 400, "json header not application");
                return;
            }

            InputStream requestBody = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            reader.close();
            requestBody.close();
            try {
                JSONObject requestBodyJson = new JSONObject(requestBodyBuilder.toString());
                if (requestBodyJson.has("query")) {
                    handleQuery(exchange, requestBodyJson);
                    return;
                }
                String nickname = requestBodyJson.getString("nickname");
                Double latitude = requestBodyJson.getDouble("latitude");
                Double longitude = requestBodyJson.getDouble("longitude");
                String dangertype = requestBodyJson.getString("dangertype");
                String sent = requestBodyJson.getString("sent");

                WarningMessage message = new WarningMessage();
                message.setNickname(nickname);
                message.setLatitude(latitude);
                message.setLongitude(longitude);
                message.setDangertype(dangertype);
                if (!message.setSent(sent)) {
                    sendResponse(exchange, 400, "Invalid time format");
                    return;
                }
                if ((!dangertype.equals("Deer") && (!dangertype.equals("Moose")
                        && (!dangertype.equals("Reindeer") && (!dangertype.equals("Other")))))) {
                    sendResponse(exchange, 400, "Invalid dangertype");
                    return;
                }
                if (requestBodyJson.has("areacode")) {
                    message.setAreacode(requestBodyJson.getString("areacode"));
                }
                if (requestBodyJson.has("phonenumber")) {
                    message.setPhonenumber(requestBodyJson.getString("phonenumber"));
                }
                if (requestBodyJson.has("weather")) {
                    WeatherClient weather = new WeatherClient();
                    String weatherString = weather.getWeather(latitude, longitude);
                    message.setWeather(weatherString);
                }
                db.open(DBNAME);
                if (!db.addMessage(message)) {
                    sendResponse(exchange, 400, "Something went wrong while adding message");
                }
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            } catch (JSONException e) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            } catch (SQLException sqe){}
            // Implementation of GET
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            JSONArray array = new JSONArray();
            try {
                db.open(DBNAME);
                array = db.getMessages();
                db.close();
                sendResponse(exchange, 200, array.toString());
            } catch (SQLException sqe) {
            }
            // Implementation of other inputs
        } else {
            exchange.sendResponseHeaders(400, "Not supported".length());
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Not supported".getBytes());
            outputStream.flush();
            outputStream.close();
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

    private void handleQuery(HttpExchange exchange, final JSONObject requestBodyJson)
            throws JSONException, SQLException {
        JSONArray array = new JSONArray();
        switch (requestBodyJson.getString("query")) {
            case ("time"):
                WarningMessage msg = new WarningMessage();
                msg.setSent(requestBodyJson.getString("timestart"));
                long start = msg.getLongTime();
                msg.setSent(requestBodyJson.getString("timeend"));
                long end = msg.getLongTime();
                db.open(DBNAME);
                array = db.getMessagesWithinTimePeriod(start, end);
                break;

            case ("location"):
                double longitudedown = requestBodyJson.getDouble("downlongitude");
                double longitudeup = requestBodyJson.getDouble("uplongitude");
                double latitudedown = requestBodyJson.getDouble("downlatitude");
                double latitudeup = requestBodyJson.getDouble("uplatitude");
                if (longitudedown > longitudeup) {
                    double temp = longitudedown;
                    longitudedown = longitudeup;
                    longitudeup = temp;
                }
                if (latitudedown > latitudeup) {
                    double temp = latitudedown;
                    latitudedown = latitudeup;
                    latitudeup = temp;
                }
                db.open(DBNAME);
                array = db.getMessagesInArea(longitudeup, longitudedown, latitudeup, latitudedown);
                break;
            case ("user"):
                db.open(DBNAME);
                String user = requestBodyJson.getString("nickname");
                array = db.getMessagesFromUser(user);
                break;
            default:
                sendResponse(exchange, 400, "Invalid query value");
                return;
        }
        db.close();
        sendResponse(exchange, 200, array.toString());

    }
}
