package database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
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

    private static boolean firebaseInitialized = false;

    // Initialize Firebase Admin SDK
    private void initFirebaseAdmin() {
        if (firebaseInitialized)
            return;

        try {
            java.io.File file = new java.io.File(SERVICE_ACCOUNT_PATH);
            System.out.println("Looking for service account at: " + file.getAbsolutePath());

            if (!file.exists()) {
                System.out.println("ERROR: serviceAccountKey.json not found!");
                System.out.println("Please download it from Firebase Console -> Project Settings -> Service Accounts");
                return;
            }

            FileInputStream serviceAccount = new FileInputStream(file);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(PROJECT_ID)
                    .build();

            FirebaseApp.initializeApp(options);
            firebaseInitialized = true;
            System.out.println("Firebase Admin SDK initialized successfully!");
        } catch (Exception e) {
            System.out.println("Firebase Admin init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    // Delete employee from Firestore and Firebase Auth
    public boolean deleteEmployee(String uid) {
        boolean firestoreDeleted = false;
        boolean authDeleted = false;
        boolean payrollDeleted = false;

        // First, delete all payroll entries for this employee
        try {
            payrollDeleted = deletePayrollByUserId(uid);
            System.out.println("Payroll entries delete: " + (payrollDeleted ? "Success" : "No entries or failed"));
        } catch (Exception e) {
            System.out.println("Payroll Delete Error: " + e.getMessage());
        }

        // Delete from Firestore
        try {
            URL url = new URL(FIRESTORE_URL + "/users/" + uid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");

            int code = conn.getResponseCode();
            firestoreDeleted = (code == 200 || code == 204);
            System.out.println("Firestore delete: " + (firestoreDeleted ? "Success" : "Failed"));

        } catch (Exception e) {
            System.out.println("Firestore Delete Error: " + e.getMessage());
        }

        // Delete from Firebase Auth using Admin SDK
        try {
            initFirebaseAdmin();
            FirebaseAuth.getInstance().deleteUser(uid);
            authDeleted = true;
            System.out.println("Firebase Auth delete: Success");

        } catch (Exception e) {
            System.out.println("Firebase Auth Delete Error: " + e.getMessage());
        }

        return firestoreDeleted && authDeleted;
    }

    // Delete all payroll entries for a specific user
    private boolean deletePayrollByUserId(String userId) {
        try {
            URL url = new URL(FIRESTORE_URL + "/Payroll_Salary");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    int deletedCount = 0;

                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String docUserId = getField(fields, "userid");
                        if (userId.equals(docUserId)) {
                            // Get payroll ID from document name
                            String name = docObj.get("name").getAsString();
                            String payrollId = name.substring(name.lastIndexOf("/") + 1);

                            // Delete this payroll entry
                            if (deletePayroll(payrollId)) {
                                deletedCount++;
                            }
                        }
                    }
                    System.out.println("Deleted " + deletedCount + " payroll entries for user " + userId);
                    return true;
                }
            }
            return true; // No documents to delete is also success

        } catch (Exception e) {
            System.out.println("Delete Payroll By User Error: " + e.getMessage());
            return false;
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

    // Helper method for double values
    private JsonObject doubleValue(double value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("doubleValue", value);
        return obj;
    }

    // Get month name from month number
    private String getMonthName(String month) {
        String[] months = { "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December" };
        try {
            int m = Integer.parseInt(month);
            if (m >= 1 && m <= 12) {
                return months[m - 1];
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return month;
    }

    // Add payroll entry
    public String addPayroll(String userId, double salary, String monthEntry, String yearEntry) {
        try {
            // Validate month
            int month = Integer.parseInt(monthEntry);
            if (month < 1 || month > 12) {
                return "Invalid month. Please enter a value between 01 and 12.";
            }

            // Validate year
            int year = Integer.parseInt(yearEntry);
            if (year < 1900 || year > 2100) {
                return "Invalid year. Please enter a valid 4-digit year.";
            }

            // Format month as 2 digits
            String formattedMonth = String.format("%02d", month);

            // Check for duplicate entry (same user + month + year)
            URL checkUrl = new URL(FIRESTORE_URL + "/Payroll_Salary");
            HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
            checkConn.setRequestMethod("GET");

            if (checkConn.getResponseCode() == 200) {
                String response = readResponse(checkConn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String existingUserId = getField(fields, "userid");
                        String existingMonth = getField(fields, "Month_Entry");
                        String existingYear = getField(fields, "Year_Entry");

                        if (userId.equals(existingUserId) &&
                                formattedMonth.equals(existingMonth) &&
                                yearEntry.equals(existingYear)) {
                            return "Payroll entry already exists for this employee in " +
                                    getMonthName(formattedMonth) + " " + yearEntry;
                        }
                    }
                }
            }

            // Generate payroll ID
            String payrollId = "payroll_" + System.currentTimeMillis() + "_" +
                    java.util.UUID.randomUUID().toString().substring(0, 8);

            // Create payroll document
            URL url = new URL(FIRESTORE_URL + "/Payroll_Salary?documentId=" + payrollId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("payroll_id", stringValue(payrollId));
            fields.add("userid", stringValue(userId));
            fields.add("Salary", doubleValue(salary));
            fields.add("Month_Entry", stringValue(formattedMonth));
            fields.add("Year_Entry", stringValue(yearEntry));

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                return "Payroll entry added successfully! ID: " + payrollId;
            } else {
                return "Failed to add payroll entry.";
            }

        } catch (NumberFormatException e) {
            return "Invalid month or year format. Please enter numeric values.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Get all payroll entries
    public String getAllPayroll() {
        try {
            URL url = new URL(FIRESTORE_URL + "/Payroll_Salary");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                StringBuilder result = new StringBuilder();
                result.append("========================================\n");
                result.append("           ALL PAYROLL ENTRIES\n");
                result.append("========================================\n");

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    int count = 1;
                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String payrollId = getField(fields, "payroll_id");
                        String userId = getField(fields, "userid");
                        double salary = getDoubleField(fields, "Salary");
                        String monthEntry = getField(fields, "Month_Entry");
                        String yearEntry = getField(fields, "Year_Entry");

                        // Get employee info
                        String employeeInfo = getEmployeeNameAndEmail(userId);

                        result.append("\n[").append(count++).append("]\n");
                        result.append("Payroll ID  : ").append(payrollId).append("\n");
                        result.append("Employee    : ").append(employeeInfo).append("\n");
                        result.append("Salary      : RM ").append(String.format("%,.2f", salary)).append("\n");
                        result.append("Month/Year  : ").append(getMonthName(monthEntry)).append(" ").append(yearEntry)
                                .append("\n");
                        result.append("----------------------------------------");
                    }
                    if (count == 1) {
                        result.append("\nNo payroll entries found.\n");
                    }
                } else {
                    result.append("\nNo payroll entries found.\n");
                }
                result.append("\n========================================");
                return result.toString();
            }
            return "Failed to get payroll entries.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Get payroll by user ID
    public String getPayrollByUserId(String userId) {
        try {
            URL url = new URL(FIRESTORE_URL + "/Payroll_Salary");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                // Get employee info
                String employeeInfo = getEmployeeNameAndEmail(userId);

                StringBuilder result = new StringBuilder();
                result.append("========================================\n");
                result.append("        PAYROLL HISTORY\n");
                result.append("========================================\n");
                result.append("Employee: ").append(employeeInfo).append("\n");
                result.append("----------------------------------------");

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    int count = 1;
                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String docUserId = getField(fields, "userid");
                        if (!userId.equals(docUserId)) {
                            continue;
                        }

                        String payrollId = getField(fields, "payroll_id");
                        double salary = getDoubleField(fields, "Salary");
                        String monthEntry = getField(fields, "Month_Entry");
                        String yearEntry = getField(fields, "Year_Entry");

                        result.append("\n[").append(count++).append("]\n");
                        result.append("Payroll ID  : ").append(payrollId).append("\n");
                        result.append("Salary      : RM ").append(String.format("%,.2f", salary)).append("\n");
                        result.append("Month/Year  : ").append(getMonthName(monthEntry)).append(" ").append(yearEntry)
                                .append("\n");
                        result.append("----------------------------------------");
                    }
                    if (count == 1) {
                        result.append("\nNo payroll entries found for this employee.\n");
                        result.append("----------------------------------------");
                    }
                } else {
                    result.append("\nNo payroll entries found for this employee.\n");
                    result.append("----------------------------------------");
                }
                result.append("\n========================================");
                return result.toString();
            }
            return "Failed to get payroll entries.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Update payroll entry
    public boolean updatePayroll(String payrollId, double salary, String monthEntry, String yearEntry) {
        try {
            // Validate month
            int month = Integer.parseInt(monthEntry);
            if (month < 1 || month > 12) {
                System.out.println("Invalid month");
                return false;
            }

            // Validate year
            int year = Integer.parseInt(yearEntry);
            if (year < 1900 || year > 2100) {
                System.out.println("Invalid year");
                return false;
            }

            // Format month as 2 digits
            String formattedMonth = String.format("%02d", month);

            // First get the current payroll data (need userid)
            String userId = null;

            URL getUrl = new URL(FIRESTORE_URL + "/Payroll_Salary/" + payrollId);
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");

            if (getConn.getResponseCode() == 200) {
                String response = readResponse(getConn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject existingFields = json.getAsJsonObject("fields");
                userId = getField(existingFields, "userid");
            }

            if (userId == null) {
                System.out.println("Could not get current payroll data");
                return false;
            }

            // Delete the document
            URL deleteUrl = new URL(FIRESTORE_URL + "/Payroll_Salary/" + payrollId);
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode();

            // Recreate with updated data
            URL createUrl = new URL(FIRESTORE_URL + "/Payroll_Salary?documentId=" + payrollId);
            HttpURLConnection createConn = (HttpURLConnection) createUrl.openConnection();
            createConn.setRequestMethod("POST");
            createConn.setRequestProperty("Content-Type", "application/json");
            createConn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("payroll_id", stringValue(payrollId));
            fields.add("userid", stringValue(userId));
            fields.add("Salary", doubleValue(salary));
            fields.add("Month_Entry", stringValue(formattedMonth));
            fields.add("Year_Entry", stringValue(yearEntry));

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = createConn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = createConn.getResponseCode();
            return code == 200 || code == 201;

        } catch (Exception e) {
            System.out.println("Update Payroll Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Delete payroll entry
    public boolean deletePayroll(String payrollId) {
        try {
            URL url = new URL(FIRESTORE_URL + "/Payroll_Salary/" + payrollId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");

            int code = conn.getResponseCode();
            return code == 200 || code == 204;

        } catch (Exception e) {
            System.out.println("Delete Payroll Error: " + e.getMessage());
            return false;
        }
    }

    // Helper method to get employee name and email
    private String getEmployeeNameAndEmail(String userId) {
        try {
            URL url = new URL(FIRESTORE_URL + "/users/" + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject fields = json.getAsJsonObject("fields");

                String firstName = getField(fields, "first_name");
                String lastName = getField(fields, "last_name");
                String email = getField(fields, "email");

                return firstName + " " + lastName + " (" + email + ")";
            }
        } catch (Exception e) {
            // ignore
        }
        return "Unknown (" + userId + ")";
    }

    // Helper method to get double field value
    private double getDoubleField(JsonObject fields, String name) {
        if (fields != null && fields.has(name)) {
            JsonObject field = fields.getAsJsonObject(name);
            if (field.has("doubleValue")) {
                return field.get("doubleValue").getAsDouble();
            } else if (field.has("integerValue")) {
                return field.get("integerValue").getAsDouble();
            }
        }
        return 0.0;
    }
}
