package database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    // Add employee to Firebase Auth and Firestore
    public String addEmployee(String email, String password, String firstName, String lastName, String icPassport,
            String role) {
        try {
            // Step 1: Create user in Firebase Auth
            URL url = new URL(AUTH_SIGNUP_URL);
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
                String uid = json.get("localId").getAsString();

                // Step 2: Add user data to Firestore
                boolean firestoreSuccess = addUserToFirestore(uid, email, firstName, lastName, icPassport, role);

                if (firestoreSuccess) {
                    return "Employee added successfully! UID: " + uid;
                } else {
                    return "Auth created but Firestore failed.";
                }
            } else {
                return "Failed to add employee.";
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Add user data to Firestore
    private boolean addUserToFirestore(String uid, String email, String firstName, String lastName, String icPassport,
            String role) {
        try {
            URL url = new URL(FIRESTORE_URL + "/users?documentId=" + uid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("email", stringValue(email));
            fields.add("first_name", stringValue(firstName));
            fields.add("last_name", stringValue(lastName));
            fields.add("ic_passport", stringValue(icPassport));
            fields.add("role", stringValue(role));

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            return code == 200 || code == 201;

        } catch (Exception e) {
            System.out.println("Firestore Error: " + e.getMessage());
            return false;
        }
    }

    // Get all employees from Firestore
    public String getAllEmployees() {
        try {
            URL url = new URL(FIRESTORE_URL + "/users");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                StringBuilder result = new StringBuilder();
                result.append("========================================\n");
                result.append("           ALL EMPLOYEES\n");
                result.append("========================================\n");

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    int count = 1;
                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        // Only show employees, not HR
                        String role = getField(fields, "role");
                        if (!"employee".equalsIgnoreCase(role)) {
                            continue;
                        }

                        // Get UID from document name
                        String name = docObj.get("name").getAsString();
                        String uid = name.substring(name.lastIndexOf("/") + 1);

                        result.append("\n[").append(count++).append("]\n");
                        result.append("UID         : ").append(uid).append("\n");
                        result.append("Email       : ").append(getField(fields, "email")).append("\n");
                        result.append("First Name  : ").append(getField(fields, "first_name")).append("\n");
                        result.append("Last Name   : ").append(getField(fields, "last_name")).append("\n");
                        result.append("IC/Passport : ").append(getField(fields, "ic_passport")).append("\n");
                        result.append("Role        : ").append(role).append("\n");
                    }
                    if (count == 1) {
                        result.append("No employees found.\n");
                    }
                } else {
                    result.append("No employees found.\n");
                }
                result.append("========================================");
                return result.toString();
            }
            return "Failed to get employees.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Update employee in Firestore (delete and recreate to avoid PATCH issues)
    public boolean updateEmployee(String uid, String firstName, String lastName, String icPassport, String role) {
        try {
            // First get the current data (email should not change)
            String email = null;
            URL getUrl = new URL(FIRESTORE_URL + "/users/" + uid);
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");

            if (getConn.getResponseCode() == 200) {
                String response = readResponse(getConn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject existingFields = json.getAsJsonObject("fields");
                email = getField(existingFields, "email");
            }

            if (email == null) {
                System.out.println("Could not get current employee data");
                return false;
            }

            // Delete the document
            URL deleteUrl = new URL(FIRESTORE_URL + "/users/" + uid);
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode(); // Execute delete

            // Recreate with updated data
            URL createUrl = new URL(FIRESTORE_URL + "/users?documentId=" + uid);
            HttpURLConnection createConn = (HttpURLConnection) createUrl.openConnection();
            createConn.setRequestMethod("POST");
            createConn.setRequestProperty("Content-Type", "application/json");
            createConn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("email", stringValue(email));
            fields.add("first_name", stringValue(firstName));
            fields.add("last_name", stringValue(lastName));
            fields.add("ic_passport", stringValue(icPassport));
            fields.add("role", stringValue(role));

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = createConn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = createConn.getResponseCode();
            return code == 200 || code == 201;

        } catch (Exception e) {
            System.out.println("Update Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get employee by UID
    public String getEmployeeByUid(String uid) {
        try {
            URL url = new URL(FIRESTORE_URL + "/users/" + uid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject fields = json.getAsJsonObject("fields");

                StringBuilder result = new StringBuilder();
                result.append("========================================\n");
                result.append("         EMPLOYEE DETAILS\n");
                result.append("========================================\n");
                result.append("UID         : ").append(uid).append("\n");
                result.append("Email       : ").append(getField(fields, "email")).append("\n");
                result.append("First Name  : ").append(getField(fields, "first_name")).append("\n");
                result.append("Last Name   : ").append(getField(fields, "last_name")).append("\n");
                result.append("IC/Passport : ").append(getField(fields, "ic_passport")).append("\n");
                result.append("Role        : ").append(getField(fields, "role")).append("\n");
                result.append("========================================");
                return result.toString();
            }
            return "Employee not found.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Helper methods
    private JsonObject stringValue(String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("stringValue", value);
        return obj;
    }

    private String getField(JsonObject fields, String name) {
        if (fields != null && fields.has(name)) {
            return fields.getAsJsonObject(name).get("stringValue").getAsString();
        }
        return "N/A";
    }

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
