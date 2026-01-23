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
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * AuthService - Business Logic Layer (Firebase Operations)
 *
 * This class handles ALL communication with Firebase:
 * - Firebase Authentication (login, signup, delete user)
 * - Firestore Database (CRUD for users, payroll)
 *
 * Uses TWO different approaches:
 * 1. REST API - For login, signup, and Firestore operations (using HTTP requests)
 * 2. Admin SDK - For deleting users from Firebase Auth (requires serviceAccountKey.json)
 *
 * This class is called by AuthServiceImpl, which is called via RMI from the client.
 */
public class AuthService {

    // ==================== FIREBASE CONFIGURATION ====================

    // Firebase Web API Key (from Firebase Console -> Project Settings)
    private static final String API_KEY = "AIzaSyCQEVRTuYHIL2YAiEtP6dIqhzblIRU28dA";

    // Firebase Project ID
    private static final String PROJECT_ID = "distributed-system-data";

    // Path to service account key (needed for Admin SDK - delete users)
    private static final String SERVICE_ACCOUNT_PATH = "serviceAccountKey.json";

    // Firebase REST API URLs
    private static final String AUTH_LOGIN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
            + API_KEY;

    private static final String AUTH_SIGNUP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key="
            + API_KEY;

    // Firestore REST API base URL
    private static final String FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
            + "/databases/(default)/documents";

    // Track if Firebase Admin SDK has been initialized
    private static boolean firebaseInitialized = false;

    // ==================== FIREBASE ADMIN SDK INITIALIZATION ====================

    /**
     * Initialize Firebase Admin SDK
     * Required for operations that need admin privileges (like deleting users)
     * Uses serviceAccountKey.json for authentication
     */
    private void initFirebaseAdmin() {
        if (firebaseInitialized)
            return;  // Only initialize once

        try {
            java.io.File file = new java.io.File(SERVICE_ACCOUNT_PATH);
            System.out.println("Looking for service account at: " + file.getAbsolutePath());

            if (!file.exists()) {
                System.out.println("ERROR: serviceAccountKey.json not found!");
                System.out.println("Please download it from Firebase Console -> Project Settings -> Service Accounts");
                return;
            }

            // Load credentials from service account file
            FileInputStream serviceAccount = new FileInputStream(file);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(PROJECT_ID)
                    .build();

            // Initialize the Firebase app
            FirebaseApp.initializeApp(options);
            firebaseInitialized = true;
            System.out.println("Firebase Admin SDK initialized successfully!");
        } catch (Exception e) {
            System.out.println("Firebase Admin init error: " + e.getMessage());
        }
    }

    // ==================== AUTHENTICATION METHODS ====================

    /**
     * Login user with Firebase Authentication REST API
     * Sends POST request to Firebase Auth endpoint
     * @return User's UID if successful, null if failed
     */
    public String login(String email, String password) {
        try {
            URL url = URI.create(AUTH_LOGIN_URL).toURL();
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

    /**
     * Get user's role from Firestore
     * Reads the "role" field from /users/{uid} document
     * @return "hr" or "employee", null if not found
     */
    public String getRole(String uid) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
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

    // ==================== EMPLOYEE CRUD METHODS ====================

    /**
     * Add new employee to the system
     * Step 1: Create user in Firebase Auth (signup)
     * Step 2: Add user data to Firestore /users collection
     * @return Success/error message
     */
    public String addEmployee(String email, String password, String firstName, String lastName, String icPassport,
            String role) {
        try {
            // Step 1: Create user in Firebase Auth
            URL url = URI.create(AUTH_SIGNUP_URL).toURL();
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
                String error = readErrorResponse(conn);

                // Parse error and show user-friendly message
                if (error.contains("EMAIL_EXISTS")) {
                    return "Email already exists. Please try another email.";
                } else if (error.contains("INVALID_EMAIL")) {
                    return "Invalid email format. Please enter a valid email.";
                } else if (error.contains("WEAK_PASSWORD")) {
                    return "Password is too weak. Please use at least 6 characters.";
                } else {
                    return "Failed to add employee. Please try again.";
                }
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Helper: Add user document to Firestore /users collection
     * Called after creating user in Firebase Auth
     */
    private boolean addUserToFirestore(String uid, String email, String firstName, String lastName, String icPassport,
            String role) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/users?documentId=" + uid).toURL();
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

    /**
     * Get all employees from Firestore /users collection
     * Filters to only show users with role "employee" (not HR)
     * @return Formatted string with all employee data
     */
    public String getAllEmployees() {
        try {
            URL url = URI.create(FIRESTORE_URL + "/users").toURL();
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

    /**
     * Delete employee from the system completely
     * Step 1: Delete all payroll entries for this employee
     * Step 2: Delete from Firestore /users collection
     * Step 3: Delete from Firebase Auth (requires Admin SDK)
     * @return true if all deletions successful
     */
    public boolean deleteEmployee(String uid) {
        boolean firestoreDeleted = false;
        boolean authDeleted = false;

        // First, delete all payroll entries for this employee
        try {
            boolean payrollDeleted = deletePayrollByUserId(uid);
            System.out.println("Payroll entries delete: " + (payrollDeleted ? "Success" : "No entries or failed"));
        } catch (Exception e) {
            System.out.println("Payroll Delete Error: " + e.getMessage());
        }

        // Delete from Firestore
        try {
            URL url = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
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

    /**
     * Helper: Delete all payroll entries for a specific user
     * Called when deleting an employee to clean up their payroll data
     */
    private boolean deletePayrollByUserId(String userId) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/Payroll_Salary").toURL();
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

    /**
     * Update employee data in Firestore
     * Uses delete-and-recreate approach to avoid PATCH issues
     * Email cannot be changed (read from existing document)
     */
    public boolean updateEmployee(String uid, String firstName, String lastName, String icPassport, String role) {
        try {
            // First get the current data (email should not change)
            String email = null;
            URL getUrl = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
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
            URL deleteUrl = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode(); // Execute delete

            // Recreate with updated data
            URL createUrl = URI.create(FIRESTORE_URL + "/users?documentId=" + uid).toURL();
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
            return false;
        }
    }

    /**
     * Get single employee by UID from Firestore
     * @return Formatted string with employee details
     */
    public String getEmployeeByUid(String uid) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
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

    // ==================== HELPER METHODS ====================
    // These methods help format data for Firestore REST API

    /**
     * Helper: Create Firestore string value object
     * Firestore REST API requires: {"stringValue": "actual value"}
     */
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
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * Helper: Create Firestore double value object
     * Firestore REST API requires: {"doubleValue": 123.45}
     */
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

    // ==================== PAYROLL CRUD METHODS ====================
    // These methods handle the Payroll_Salary collection in Firestore

    /**
     * Add new payroll entry to Firestore
     * Validates month/year and checks for duplicate entries
     * @return Success/error message
     */
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
            URL checkUrl = URI.create(FIRESTORE_URL + "/Payroll_Salary").toURL();
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
            URL url = URI.create(FIRESTORE_URL + "/Payroll_Salary?documentId=" + payrollId).toURL();
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

    /**
     * Get all payroll entries from Firestore
     * Includes employee name lookup for each entry
     * @return Formatted string with all payroll data
     */
    public String getAllPayroll() {
        try {
            URL url = URI.create(FIRESTORE_URL + "/Payroll_Salary").toURL();
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

    /**
     * Get payroll history for a specific employee
     * Filters entries by userId
     * @return Formatted string with employee's payroll history
     */
    public String getPayrollByUserId(String userId) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/Payroll_Salary").toURL();
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

    /**
     * Update existing payroll entry
     * Uses delete-and-recreate approach
     * Validates month/year before updating
     */
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

            URL getUrl = URI.create(FIRESTORE_URL + "/Payroll_Salary/" + payrollId).toURL();
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
            URL deleteUrl = URI.create(FIRESTORE_URL + "/Payroll_Salary/" + payrollId).toURL();
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode();

            // Recreate with updated data
            URL createUrl = URI.create(FIRESTORE_URL + "/Payroll_Salary?documentId=" + payrollId).toURL();
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
            return false;
        }
    }

    /**
     * Delete a payroll entry from Firestore
     * @param payrollId The ID of the payroll entry to delete
     * @return true if deleted successfully
     */
    public boolean deletePayroll(String payrollId) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/Payroll_Salary/" + payrollId).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");

            int code = conn.getResponseCode();
            return code == 200 || code == 204;

        } catch (Exception e) {
            System.out.println("Delete Payroll Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper: Get employee name and email for display
     * Used when showing payroll entries to identify the employee
     */
    private String getEmployeeNameAndEmail(String userId) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/users/" + userId).toURL();
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

    /**
     * Helper: Extract double value from Firestore JSON response
     * Handles both doubleValue and integerValue formats
     */
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
