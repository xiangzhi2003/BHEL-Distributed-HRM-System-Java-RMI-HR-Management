package database;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthService {

    // Firebase API Key
    private static final String API_KEY = "AIzaSyCQEVRTuYHIL2YAiEtP6dIqhzblIRU28dA";

    // Firebase Project ID
    private static final String PROJECT_ID = "distributed-system-data";

    // Path to service account key JSON file
    private static final String SERVICE_ACCOUNT_PATH = "serviceAccountKey.json";

    private static final String AUTH_LOGIN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
            + API_KEY;

    private static final String AUTH_SIGNUP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key="
            + API_KEY;

    private static final String FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
            + "/databases/(default)/documents";

    // Login with Firebase Auth
    public String login(String email, String password) {
        try {
            URL url = new URL(AUTH_LOGIN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject body = new JsonObject();
            body.addProperty("email", email);
            body.addProperty("password", password);
            body.addProperty("returnSecureToken", true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                return json.get("localId").getAsString();
            }
            return null;

        } catch (Exception e) {
            System.out.println("Auth Error: " + e.getMessage());
            return null;
        }
    }

    // Get role from Firestore
    public String getRole(String uid) {
        try {
            URL url = new URL(FIRESTORE_URL + "/users/" + uid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject fields = json.getAsJsonObject("fields");
                if (fields != null && fields.has("role")) {
                    return fields.getAsJsonObject("role").get("stringValue").getAsString();
                }
            }
            return null;

        } catch (Exception e) {
            System.out.println("Firestore Error: " + e.getMessage());
            return null;
        }
    }

    // Helper methods
    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
}
