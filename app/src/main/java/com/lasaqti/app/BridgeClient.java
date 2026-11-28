package com.lasaqti.app;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class BridgeClient {
    private static final String BASE = "https://lasaqti-bridge-mgk27z.v2.appdeploy.ai";
    private BridgeClient() {}

    public static String createRoom() throws Exception {
        JSONObject response = request("POST", "/api/rooms", new JSONObject());
        String code = response.optString("room_id", "");
        if (code.length() != 8) throw new Exception("لم يصل رمز الغرفة");
        return code;
    }

    public static void sendText(String roomCode, String text) throws Exception {
        JSONObject body = new JSONObject();
        body.put("text", text);
        request("POST", "/api/rooms/" + roomCode + "/message", body);
    }

    private static JSONObject request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder result = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
            }
        }
        connection.disconnect();
        if (status < 200 || status >= 300) throw new Exception("HTTP " + status + ": " + result);
        return new JSONObject(result.toString());
    }
}