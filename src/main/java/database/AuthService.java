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
 * 1. REST API - For login, signup, and Firestore operations (using HTTP
 * requests)
 * 2. Admin SDK - For deleting users from Firebase Auth (requires
 * serviceAccountKey.json)
 *
 * This class is called by AuthServiceImpl, which is called via RMI from the
 * client.
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
    /**
     * Initialize Firebase Admin SDK
     * Required for operations that need admin privileges (like deleting users)
     * Uses serviceAccountKey.json for authentication
     */
    private void initFirebaseAdmin() {
        if (firebaseInitialized)
            return; // Only initialize once

        try {
            // Check current directory first
            String[] pathsToCheck = {
                    SERVICE_ACCOUNT_PATH,
                    "../" + SERVICE_ACCOUNT_PATH, // Check parent directory
                    "BHEL-Distributed-HRM-System-Java-RMI-HR-Management/" + SERVICE_ACCOUNT_PATH // Check subfolder if
                                                                                                 // running from root
            };

            java.io.File file = null;
            for (String path : pathsToCheck) {
                java.io.File f = new java.io.File(path);
                System.out.println("Checking for key at: " + f.getAbsolutePath());
                if (f.exists()) {
                    file = f;
                    break;
                }
            }

            if (file == null || !file.exists()) {
                System.out
                        .println("==================================================================================");
                System.out.println("CRITICAL ERROR: serviceAccountKey.json NOT FOUND!");
                System.out
                        .println("----------------------------------------------------------------------------------");
                System.out.println("To support Email Updates, you MUST:");
                System.out.println(
                        "1. Download the private key from Firebase Console -> Project Settings -> Service Accounts");
                System.out.println("2. Rename it to 'serviceAccountKey.json'");
                System.out.println("3. Place it in THIS folder: " + new java.io.File(".").getAbsolutePath());
                System.out
                        .println("==================================================================================");
                return;
            }

            System.out.println("Found service account key at: " + file.getAbsolutePath());

            // Load credentials from service account file
            FileInputStream serviceAccount = new FileInputStream(file);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(PROJECT_ID)
                    .build();

            // Initialize the Firebase app
            // Check if already initialized to avoid duplicate app errors
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firebaseInitialized = true;
            System.out.println("Firebase Admin SDK initialized successfully!");
        } catch (java.io.IOException | IllegalStateException e) {
            System.out.println("Firebase Admin init error: " + e.getMessage());
        }
    }

    // ==================== AUTHENTICATION METHODS ====================

    /**
     * Login user with Firebase Authentication REST API
     * Sends POST request to Firebase Auth endpoint
     * 
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
            } else {
                System.out.println("Login Failed. Response Code: " + conn.getResponseCode());
                String error = readErrorResponse(conn);
                System.out.println("Error Response: " + error);
                return null;
            }

        } catch (Exception e) {
            System.out.println("Auth Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get user's role from Firestore
     * Reads the "role" field from /users/{uid} document
     * 
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
     * Step 3: Create Leave_Balance document with default values
     *
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

                // Step 3: Create Leave_Balance document for the new employee
                boolean leaveBalanceSuccess = createLeaveBalance(uid);

                if (firestoreSuccess && leaveBalanceSuccess) {
                    return "Employee added successfully! UID: " + uid;
                } else if (firestoreSuccess) {
                    return "Employee created but Leave Balance creation failed. UID: " + uid;
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

        } catch (java.io.IOException e) {
            System.out.println("Firestore Error: " + e.getMessage());
            return false;
        }
    }

    // ==================== LEAVE BALANCE METHODS ====================

    /**
     * Create Leave_Balance document for a new employee
     * Sets default values: 10 days each for annual, emergency, medical leave
     * Automatically sets current year
     *
     * @param userId Employee's UID
     * @return true if successful
     */
    private boolean createLeaveBalance(String userId) {
        try {
            // Get current year
            String currentYear = String.valueOf(java.time.Year.now().getValue());

            // Generate unique leave_balance_id
            String leaveBalanceId = "lb_" + userId.substring(0, Math.min(8, userId.length())) + "_" + currentYear;

            URL url = URI.create(FIRESTORE_URL + "/Leave_Balance?documentId=" + leaveBalanceId).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("leave_balance_id", stringValue(leaveBalanceId));
            fields.add("userid", stringValue(userId));
            fields.add("year", stringValue(currentYear));
            fields.add("annual_leave", integerValue(10));      // Default 10 days
            fields.add("emergency_leave", integerValue(10));   // Default 10 days
            fields.add("medical_leave", integerValue(10));     // Default 10 days

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                System.out.println("Leave Balance created for user: " + userId + " (Year: " + currentYear + ")");
                return true;
            } else {
                String error = readErrorResponse(conn);
                System.out.println("Failed to create Leave Balance: " + error);
                return false;
            }

        } catch (Exception e) {
            System.out.println("Create Leave Balance Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check and reset leave balance if we are in a new year
     * Called when employee logs in or accesses leave features
     *
     * @param userId Employee's UID
     * @return true if reset was performed or no reset needed
     */
    public boolean checkAndResetLeaveBalance(String userId) {
        try {
            String currentYear = String.valueOf(java.time.Year.now().getValue());

            // Get existing leave balance for this user
            URL url = URI.create(FIRESTORE_URL + "/Leave_Balance").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    boolean foundForUser = false;
                    boolean needsReset = false;
                    String existingLeaveBalanceId = null;

                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String docUserId = getField(fields, "userid");
                        if (userId.equals(docUserId)) {
                            foundForUser = true;
                            String docYear = getField(fields, "year");
                            existingLeaveBalanceId = getField(fields, "leave_balance_id");

                            // Check if year is different (new year)
                            if (!currentYear.equals(docYear)) {
                                needsReset = true;
                            }
                            break;
                        }
                    }

                    // If no leave balance exists, create one
                    if (!foundForUser) {
                        System.out.println("No Leave Balance found for user " + userId + ". Creating new one...");
                        return createLeaveBalance(userId);
                    }

                    // If year changed, reset the leave balance
                    if (needsReset && existingLeaveBalanceId != null) {
                        System.out.println("New year detected! Resetting leave balance for user " + userId);
                        return resetLeaveBalance(userId, existingLeaveBalanceId, currentYear);
                    }

                    // No reset needed
                    return true;
                } else {
                    // No documents at all, create new leave balance
                    return createLeaveBalance(userId);
                }
            }
            return false;

        } catch (Exception e) {
            System.out.println("Check Leave Balance Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reset leave balance to default values for a new year
     * Uses delete-and-recreate approach
     *
     * @param userId          Employee's UID
     * @param leaveBalanceId  Existing leave balance ID
     * @param newYear         The new year value
     * @return true if successful
     */
    private boolean resetLeaveBalance(String userId, String leaveBalanceId, String newYear) {
        try {
            // Delete the old leave balance document
            URL deleteUrl = URI.create(FIRESTORE_URL + "/Leave_Balance/" + leaveBalanceId).toURL();
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode(); // Execute delete

            // Generate new leave_balance_id for the new year
            String newLeaveBalanceId = "lb_" + userId.substring(0, Math.min(8, userId.length())) + "_" + newYear;

            // Create new leave balance with reset values
            URL createUrl = URI.create(FIRESTORE_URL + "/Leave_Balance?documentId=" + newLeaveBalanceId).toURL();
            HttpURLConnection createConn = (HttpURLConnection) createUrl.openConnection();
            createConn.setRequestMethod("POST");
            createConn.setRequestProperty("Content-Type", "application/json");
            createConn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("leave_balance_id", stringValue(newLeaveBalanceId));
            fields.add("userid", stringValue(userId));
            fields.add("year", stringValue(newYear));
            fields.add("annual_leave", integerValue(10));      // Reset to 10 days
            fields.add("emergency_leave", integerValue(10));   // Reset to 10 days
            fields.add("medical_leave", integerValue(10));     // Reset to 10 days

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = createConn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = createConn.getResponseCode();
            if (code == 200 || code == 201) {
                System.out.println("Leave Balance reset for user: " + userId + " (Year: " + newYear + ")");
                return true;
            }
            return false;

        } catch (Exception e) {
            System.out.println("Reset Leave Balance Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get leave balance for a specific employee
     * Also checks and resets if new year
     *
     * @param userId Employee's UID
     * @return Formatted string with leave balance details
     */
    public String getLeaveBalance(String userId) {
        try {
            // First check and reset if needed (new year)
            checkAndResetLeaveBalance(userId);

            URL url = URI.create(FIRESTORE_URL + "/Leave_Balance").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");

                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String docUserId = getField(fields, "userid");
                        if (userId.equals(docUserId)) {
                            String leaveBalanceId = getField(fields, "leave_balance_id");
                            String year = getField(fields, "year");
                            int annualLeave = getIntField(fields, "annual_leave");
                            int emergencyLeave = getIntField(fields, "emergency_leave");
                            int medicalLeave = getIntField(fields, "medical_leave");
                            int totalLeave = annualLeave + emergencyLeave + medicalLeave;

                            StringBuilder result = new StringBuilder();
                            result.append("========================================\n");
                            result.append("           MY LEAVE BALANCE\n");
                            result.append("========================================\n");
                            result.append("Year            : ").append(year).append("\n");
                            result.append("----------------------------------------\n");
                            result.append("Annual Leave    : ").append(annualLeave).append(" days\n");
                            result.append("Emergency Leave : ").append(emergencyLeave).append(" days\n");
                            result.append("Medical Leave   : ").append(medicalLeave).append(" days\n");
                            result.append("----------------------------------------\n");
                            result.append("Total Remaining : ").append(totalLeave).append(" days\n");
                            result.append("========================================");
                            return result.toString();
                        }
                    }
                }
                return "No leave balance found. Please contact HR.";
            }
            return "Failed to get leave balance.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get leave balance data as a Map for programmatic access
     * Returns annual, emergency, and medical leave balances
     *
     * @param userId Employee's UID
     * @return Map with keys: "annual", "emergency", "medical" (values as Integer)
     */
    public java.util.Map<String, Integer> getLeaveBalanceData(String userId) {
        java.util.Map<String, Integer> balanceMap = new java.util.HashMap<>();
        balanceMap.put("annual", 0);
        balanceMap.put("emergency", 0);
        balanceMap.put("medical", 0);

        try {
            // First check and reset if needed (new year)
            checkAndResetLeaveBalance(userId);

            URL url = URI.create(FIRESTORE_URL + "/Leave_Balance").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");

                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String docUserId = getField(fields, "userid");
                        if (userId.equals(docUserId)) {
                            int annualLeave = getIntField(fields, "annual_leave");
                            int emergencyLeave = getIntField(fields, "emergency_leave");
                            int medicalLeave = getIntField(fields, "medical_leave");

                            balanceMap.put("annual", annualLeave);
                            balanceMap.put("emergency", emergencyLeave);
                            balanceMap.put("medical", medicalLeave);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error getting leave balance data: " + e.getMessage());
        }

        return balanceMap;
    }

    /**
     * Get all employees from Firestore /users collection
     * Filters to only show users with role "employee" (not HR)
     * 
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
     * 
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

        } catch (java.io.IOException e) {
            System.out.println("Firestore Delete Error: " + e.getMessage());
        }

        // Delete from Firebase Auth using Admin SDK
        try {
            initFirebaseAdmin();
            FirebaseAuth.getInstance().deleteUser(uid);
            authDeleted = true;
            System.out.println("Firebase Auth delete: Success");

        } catch (com.google.firebase.auth.FirebaseAuthException e) {
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
     * Update own profile information (Employee only)
     * Preserves existing Role, Updates Email via Admin SDK
     */
    public boolean updateOwnProfile(String uid, String newEmail, String firstName, String lastName, String icPassport) {
        try {
            // First get the current data
            String currentEmail = null;
            String role = null;

            URL getUrl = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");

            if (getConn.getResponseCode() == 200) {
                String response = readResponse(getConn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject existingFields = json.getAsJsonObject("fields");
                currentEmail = getField(existingFields, "email");
                role = getField(existingFields, "role");
            }

            if (currentEmail == null || role == null) {
                System.out.println("Could not get current employee data for update");
                return false;
            }

            // If email is different, update it in Firebase Auth
            // NOTE: Client logic should normally prevent empty strings, but let's be safe
            String emailToSave = currentEmail;
            if (newEmail != null && !newEmail.isEmpty() && !newEmail.equals(currentEmail)) {
                try {
                    // Update Firebase Auth
                    initFirebaseAdmin();
                    com.google.firebase.auth.UserRecord.UpdateRequest request = new com.google.firebase.auth.UserRecord.UpdateRequest(
                            uid)
                            .setEmail(newEmail);
                    FirebaseAuth.getInstance().updateUser(request);
                    System.out.println("Firebase Auth Email Updated: " + newEmail);
                    emailToSave = newEmail;
                } catch (Exception e) {
                    System.out.println("Failed to update email in Auth: " + e.getMessage());
                    return false; // Abort if Auth update fails
                }
            } else if (newEmail != null && !newEmail.isEmpty()) {
                emailToSave = newEmail; // Even if same, ensure we use the non-null value
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
            fields.add("email", stringValue(emailToSave));
            fields.add("first_name", stringValue(firstName));
            fields.add("last_name", stringValue(lastName));
            fields.add("ic_passport", stringValue(icPassport));
            fields.add("role", stringValue(role)); // Preserve role

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = createConn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = createConn.getResponseCode();
            return code == 200 || code == 201;

        } catch (Exception e) {
            System.out.println("Update Own Profile Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get raw employee data (JSON format)
     * Used for client-side parsing
     */
    public String getEmployeeRaw(String uid) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/users/" + uid).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                return readResponse(conn);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Get Employee Raw Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get single employee by UID from Firestore
     * 
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
     * 
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
     * 
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
     * 
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
     * 
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

        } catch (java.io.IOException e) {
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

    /**
     * Helper: Extract integer value from Firestore JSON response
     * Handles both integerValue and doubleValue formats
     */
    private int getIntField(JsonObject fields, String name) {
        if (fields != null && fields.has(name)) {
            JsonObject field = fields.getAsJsonObject(name);
            if (field.has("integerValue")) {
                return field.get("integerValue").getAsInt();
            } else if (field.has("doubleValue")) {
                return (int) field.get("doubleValue").getAsDouble();
            }
        }
        return 0;
    }

    /**
     * Helper: Create Firestore integer value object
     * Firestore REST API requires: {"integerValue": 123}
     */
    private JsonObject integerValue(int value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("integerValue", value);
        return obj;
    }

    /**
     * Helper: Create Firestore timestamp value object
     * Firestore REST API requires: {"timestampValue": "2024-01-01T00:00:00Z"}
     */
    private JsonObject timestampValue(String isoTimestamp) {
        JsonObject obj = new JsonObject();
        obj.addProperty("timestampValue", isoTimestamp);
        return obj;
    }

    // ==================== LEAVE MANAGEMENT METHODS ====================
    // These methods handle the Leaves collection in Firestore

    /**
     * Apply for leave - Creates a new leave request in Firestore
     * Status is automatically set to "Pending"
     *
     * @param userId    Employee's UID
     * @param leaveType Type of leave ("annual" | "emergency" | "medical")
     * @param startDate Start date of leave (format: YYYY-MM-DD)
     * @param endDate   End date of leave (format: YYYY-MM-DD)
     * @param totalDays Total number of leave days
     * @param reason    Reason for leave
     * @return Success/error message
     */
    public String applyLeave(String userId, String leaveType, String startDate, String endDate, int totalDays,
            String reason) {
        try {
            // Validate leave type
            String lowerLeaveType = leaveType.toLowerCase();
            if (!lowerLeaveType.equals("annual") && !lowerLeaveType.equals("emergency")
                    && !lowerLeaveType.equals("medical")) {
                return "Invalid leave type. Please choose 'annual', 'emergency', or 'medical'.";
            }

            // Validate dates
            if (startDate == null || startDate.trim().isEmpty()) {
                return "Start date is required.";
            }
            if (endDate == null || endDate.trim().isEmpty()) {
                return "End date is required.";
            }

            // Validate total days
            if (totalDays <= 0) {
                return "Total days must be greater than 0.";
            }

            // Validate reason
            if (reason == null || reason.trim().isEmpty()) {
                return "Reason is required.";
            }

            // Generate leave ID
            String leaveId = "leave_" + System.currentTimeMillis() + "_" +
                    java.util.UUID.randomUUID().toString().substring(0, 8);

            // Get current timestamp in ISO format
            java.time.Instant now = java.time.Instant.now();
            String dateCreatedAt = now.toString();

            // Create leave document
            URL url = URI.create(FIRESTORE_URL + "/Leave_Request?documentId=" + leaveId).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject fields = new JsonObject();
            fields.add("leave_id", stringValue(leaveId));
            fields.add("userid", stringValue(userId));
            fields.add("leave_type", stringValue(lowerLeaveType));
            fields.add("start_date", stringValue(startDate));
            fields.add("end_date", stringValue(endDate));
            fields.add("total_days", integerValue(totalDays));
            fields.add("reason", stringValue(reason));
            fields.add("status", stringValue("Pending")); // Default status is Pending
            fields.add("date_created_at", stringValue(dateCreatedAt));

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                return "Leave application submitted successfully!\n" +
                        "Leave ID: " + leaveId + "\n" +
                        "Status: Pending (awaiting HR approval)";
            } else {
                String error = readErrorResponse(conn);
                System.out.println("Firestore Error: " + error);
                return "Failed to submit leave application. Please try again.";
            }

        } catch (Exception e) {
            System.out.println("Apply Leave Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get all leave applications for a specific employee
     *
     * @param userId Employee's UID
     * @return Formatted string with leave history
     */
    public String getLeavesByUserId(String userId) {
        try {
            URL url = URI.create(FIRESTORE_URL + "/Leave_Request").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                // Get employee info
                String employeeInfo = getEmployeeNameAndEmail(userId);

                StringBuilder result = new StringBuilder();
                result.append("========================================\n");
                result.append("           MY LEAVE HISTORY\n");
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

                        String leaveId = getField(fields, "leave_id");
                        String leaveType = getField(fields, "leave_type");
                        String startDate = getField(fields, "start_date");
                        String endDate = getField(fields, "end_date");
                        int totalDays = getIntField(fields, "total_days");
                        String reason = getField(fields, "reason");
                        String status = getField(fields, "status");

                        result.append("\n[").append(count++).append("]\n");
                        result.append("Leave ID    : ").append(leaveId).append("\n");
                        result.append("Type        : ").append(capitalizeFirst(leaveType)).append("\n");
                        result.append("Period      : ").append(startDate).append(" to ").append(endDate).append("\n");
                        result.append("Total Days  : ").append(totalDays).append("\n");
                        result.append("Reason      : ").append(reason).append("\n");
                        result.append("Status      : ").append(getStatusDisplay(status)).append("\n");
                        result.append("----------------------------------------");
                    }
                    if (count == 1) {
                        result.append("\nNo leave applications found.\n");
                        result.append("----------------------------------------");
                    }
                } else {
                    result.append("\nNo leave applications found.\n");
                    result.append("----------------------------------------");
                }
                result.append("\n========================================");
                return result.toString();
            }
            return "Failed to get leave history.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Approve a leave request
     * Checks if employee has sufficient balance before approving
     * Deducts leave days from appropriate leave type upon approval
     *
     * @param leaveId Leave request ID
     * @return Success/error message
     */
    public String approveLeave(String leaveId) {
        try {
            // Step 1: Get the leave request details
            URL getUrl = URI.create(FIRESTORE_URL + "/Leave_Request/" + leaveId).toURL();
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");

            if (getConn.getResponseCode() != 200) {
                return "Leave request not found.";
            }

            String response = readResponse(getConn);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject fields = json.getAsJsonObject("fields");

            String userId = getField(fields, "userid");
            String leaveType = getField(fields, "leave_type");
            int totalDays = getIntField(fields, "total_days");
            String status = getField(fields, "status");
            String startDate = getField(fields, "start_date");
            String endDate = getField(fields, "end_date");
            String reason = getField(fields, "reason");
            String dateCreated = getField(fields, "date_created_at");

            // Check if already processed
            if (!"Pending".equalsIgnoreCase(status)) {
                return "This leave request has already been " + status.toLowerCase() + ".";
            }

            // Step 2: Check leave balance
            java.util.Map<String, Integer> balanceData = getLeaveBalanceData(userId);
            int currentBalance;
            String balanceField;

            switch (leaveType.toLowerCase()) {
                case "annual":
                    currentBalance = balanceData.get("annual");
                    balanceField = "annual_leave";
                    break;
                case "emergency":
                    currentBalance = balanceData.get("emergency");
                    balanceField = "emergency_leave";
                    break;
                case "medical":
                    currentBalance = balanceData.get("medical");
                    balanceField = "medical_leave";
                    break;
                default:
                    return "Invalid leave type: " + leaveType;
            }

            // Check if sufficient balance
            if (currentBalance < totalDays) {
                return "Insufficient leave balance! Employee has " + currentBalance + 
                       " days of " + capitalizeFirst(leaveType) + " leave, but requested " + totalDays + " days.\n" +
                       "Cannot approve. Consider rejecting this request.";
            }

            // Step 3: Deduct from leave balance
            boolean balanceDeducted = deductLeaveBalance(userId, leaveType, totalDays);
            if (!balanceDeducted) {
                return "Failed to deduct leave balance. Approval cancelled.";
            }

            // Step 4: Update leave request status to Approved (delete and recreate)
            URL deleteUrl = URI.create(FIRESTORE_URL + "/Leave_Request/" + leaveId).toURL();
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode();

            URL createUrl = URI.create(FIRESTORE_URL + "/Leave_Request?documentId=" + leaveId).toURL();
            HttpURLConnection createConn = (HttpURLConnection) createUrl.openConnection();
            createConn.setRequestMethod("POST");
            createConn.setRequestProperty("Content-Type", "application/json");
            createConn.setDoOutput(true);

            JsonObject newFields = new JsonObject();
            newFields.add("leave_id", stringValue(leaveId));
            newFields.add("userid", stringValue(userId));
            newFields.add("leave_type", stringValue(leaveType));
            newFields.add("start_date", stringValue(startDate));
            newFields.add("end_date", stringValue(endDate));
            newFields.add("total_days", integerValue(totalDays));
            newFields.add("reason", stringValue(reason));
            newFields.add("status", stringValue("Approved"));
            newFields.add("date_created_at", stringValue(dateCreated));

            JsonObject doc = new JsonObject();
            doc.add("fields", newFields);

            try (OutputStream os = createConn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = createConn.getResponseCode();
            if (code == 200 || code == 201) {
                return "Leave request APPROVED successfully!\n" +
                       "Employee: " + getEmployeeNameAndEmail(userId) + "\n" +
                       "Leave Type: " + capitalizeFirst(leaveType) + "\n" +
                       "Days Deducted: " + totalDays + "\n" +
                       "Remaining Balance: " + (currentBalance - totalDays) + " days";
            } else {
                return "Failed to update leave status.";
            }

        } catch (Exception e) {
            System.out.println("Approve Leave Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Reject a leave request
     * No balance deduction occurs when rejecting
     *
     * @param leaveId Leave request ID
     * @return Success/error message
     */
    public String rejectLeave(String leaveId) {
        try {
            // Step 1: Get the leave request details
            URL getUrl = URI.create(FIRESTORE_URL + "/Leave_Request/" + leaveId).toURL();
            HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            getConn.setRequestMethod("GET");

            if (getConn.getResponseCode() != 200) {
                return "Leave request not found.";
            }

            String response = readResponse(getConn);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject fields = json.getAsJsonObject("fields");

            String userId = getField(fields, "userid");
            String leaveType = getField(fields, "leave_type");
            int totalDays = getIntField(fields, "total_days");
            String status = getField(fields, "status");
            String startDate = getField(fields, "start_date");
            String endDate = getField(fields, "end_date");
            String reason = getField(fields, "reason");
            String dateCreated = getField(fields, "date_created_at");

            // Check if already processed
            if (!"Pending".equalsIgnoreCase(status)) {
                return "This leave request has already been " + status.toLowerCase() + ".";
            }

            // Step 2: Update leave request status to Rejected (delete and recreate)
            URL deleteUrl = URI.create(FIRESTORE_URL + "/Leave_Request/" + leaveId).toURL();
            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
            deleteConn.setRequestMethod("DELETE");
            deleteConn.getResponseCode();

            URL createUrl = URI.create(FIRESTORE_URL + "/Leave_Request?documentId=" + leaveId).toURL();
            HttpURLConnection createConn = (HttpURLConnection) createUrl.openConnection();
            createConn.setRequestMethod("POST");
            createConn.setRequestProperty("Content-Type", "application/json");
            createConn.setDoOutput(true);

            JsonObject newFields = new JsonObject();
            newFields.add("leave_id", stringValue(leaveId));
            newFields.add("userid", stringValue(userId));
            newFields.add("leave_type", stringValue(leaveType));
            newFields.add("start_date", stringValue(startDate));
            newFields.add("end_date", stringValue(endDate));
            newFields.add("total_days", integerValue(totalDays));
            newFields.add("reason", stringValue(reason));
            newFields.add("status", stringValue("Rejected"));
            newFields.add("date_created_at", stringValue(dateCreated));

            JsonObject doc = new JsonObject();
            doc.add("fields", newFields);

            try (OutputStream os = createConn.getOutputStream()) {
                os.write(doc.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = createConn.getResponseCode();
            if (code == 200 || code == 201) {
                return "Leave request REJECTED successfully!\n" +
                       "Employee: " + getEmployeeNameAndEmail(userId) + "\n" +
                       "Leave Type: " + capitalizeFirst(leaveType) + "\n" +
                       "Days: " + totalDays + " (No deduction - request rejected)";
            } else {
                return "Failed to update leave status.";
            }

        } catch (Exception e) {
            System.out.println("Reject Leave Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Generate yearly leave report
     * Aggregates all leave data for the specified year and exports to text file
     *
     * @param year Year to generate report for (e.g., "2026")
     * @param outputPath File path to save report (if null, uses current directory)
     * @return Success/error message with file path
     */
    public String generateYearlyReport(String year, String outputPath) {
        try {
            // Fetch all leave requests
            URL url = URI.create(FIRESTORE_URL + "/Leave_Request").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                return "Failed to fetch leave data.";
            }

            String response = readResponse(conn);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            // Data structures for statistics
            int totalRequests = 0;
            int pendingCount = 0;
            int approvedCount = 0;
            int rejectedCount = 0;
            int annualCount = 0;
            int emergencyCount = 0;
            int medicalCount = 0;
            int totalDaysRequested = 0;
            java.util.Map<String, Integer> employeeRequestCount = new java.util.HashMap<>();
            java.util.Map<String, String> employeeNames = new java.util.HashMap<>();
            int[] monthlyDistribution = new int[12];

            // Process leave requests
            if (json.has("documents")) {
                JsonArray docs = json.getAsJsonArray("documents");

                for (JsonElement doc : docs) {
                    JsonObject docObj = doc.getAsJsonObject();
                    JsonObject fields = docObj.getAsJsonObject("fields");

                    String dateCreated = getField(fields, "date_created_at");
                    
                    // Skip if date is invalid
                    if (dateCreated == null || dateCreated.length() < 4) {
                        continue;
                    }
                    
                    String requestYear = dateCreated.substring(0, 4);

                    // Filter by year
                    if (!year.equals(requestYear)) {
                        continue;
                    }

                    totalRequests++;

                    // Status breakdown
                    String status = getField(fields, "status");
                    if ("Pending".equalsIgnoreCase(status)) {
                        pendingCount++;
                    } else if ("Approved".equalsIgnoreCase(status)) {
                        approvedCount++;
                    } else if ("Rejected".equalsIgnoreCase(status)) {
                        rejectedCount++;
                    }

                    // Leave type breakdown
                    String leaveType = getField(fields, "leave_type");
                    if ("annual".equalsIgnoreCase(leaveType)) {
                        annualCount++;
                    } else if ("emergency".equalsIgnoreCase(leaveType)) {
                        emergencyCount++;
                    } else if ("medical".equalsIgnoreCase(leaveType)) {
                        medicalCount++;
                    }

                    // Total days
                    int totalDays = getIntField(fields, "total_days");
                    totalDaysRequested += totalDays;

                    // Employee request count
                    String userId = getField(fields, "userid");
                    employeeRequestCount.put(userId, employeeRequestCount.getOrDefault(userId, 0) + 1);
                    if (!employeeNames.containsKey(userId)) {
                        employeeNames.put(userId, getEmployeeNameAndEmail(userId));
                    }

                    // Monthly distribution
                    try {
                        int month = Integer.parseInt(dateCreated.substring(5, 7));
                        if (month >= 1 && month <= 12) {
                            monthlyDistribution[month - 1]++;
                        }
                    } catch (Exception e) {
                        // Skip if date parsing fails
                    }
                }
            }

            // Calculate statistics
            double approvalRate = totalRequests > 0 ? (approvedCount * 100.0 / totalRequests) : 0;
            double rejectionRate = totalRequests > 0 ? (rejectedCount * 100.0 / totalRequests) : 0;
            double avgDaysPerRequest = totalRequests > 0 ? (totalDaysRequested * 1.0 / totalRequests) : 0;

            // Find top 5 applicants
            java.util.List<java.util.Map.Entry<String, Integer>> topApplicants = new java.util.ArrayList<>(employeeRequestCount.entrySet());
            topApplicants.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            // Build report content
            StringBuilder report = new StringBuilder();
            report.append("========================================\n");
            report.append("    YEARLY LEAVE REPORT - ").append(year).append("\n");
            report.append("========================================\n");
            report.append("Generated on: ").append(java.time.LocalDateTime.now().toString().substring(0, 19)).append("\n");
            report.append("\n");

            // Overall Statistics
            report.append("----------------------------------------\n");
            report.append("OVERALL STATISTICS\n");
            report.append("----------------------------------------\n");
            report.append("Total Leave Requests : ").append(totalRequests).append("\n");
            report.append("Total Days Requested : ").append(totalDaysRequested).append(" days\n");
            report.append("Average Days/Request : ").append(String.format("%.2f", avgDaysPerRequest)).append(" days\n");
            report.append("\n");

            // Status Breakdown
            report.append("----------------------------------------\n");
            report.append("STATUS BREAKDOWN\n");
            report.append("----------------------------------------\n");
            report.append("Pending  : ").append(pendingCount).append(" (").append(String.format("%.1f", totalRequests > 0 ? pendingCount * 100.0 / totalRequests : 0)).append("%%)\n");
            report.append("Approved : ").append(approvedCount).append(" (").append(String.format("%.1f", approvalRate)).append("%%)\n");
            report.append("Rejected : ").append(rejectedCount).append(" (").append(String.format("%.1f", rejectionRate)).append("%%)\n");
            report.append("\n");

            // Leave Type Breakdown
            report.append("----------------------------------------\n");
            report.append("LEAVE TYPE BREAKDOWN\n");
            report.append("----------------------------------------\n");
            report.append("Annual Leave    : ").append(annualCount).append(" requests\n");
            report.append("Emergency Leave : ").append(emergencyCount).append(" requests\n");
            report.append("Medical Leave   : ").append(medicalCount).append(" requests\n");
            report.append("\n");

            // Monthly Trends
            report.append("----------------------------------------\n");
            report.append("MONTHLY DISTRIBUTION\n");
            report.append("----------------------------------------\n");
            String[] monthNames = {"January", "February", "March", "April", "May", "June",
                                   "July", "August", "September", "October", "November", "December"};
            for (int i = 0; i < 12; i++) {
                if (monthlyDistribution[i] > 0) {
                    report.append(String.format("%-12s: %d requests\n", monthNames[i], monthlyDistribution[i]));
                }
            }
            report.append("\n");

            // Top Applicants
            report.append("----------------------------------------\n");
            report.append("TOP APPLICANTS\n");
            report.append("----------------------------------------\n");
            int rank = 1;
            for (int i = 0; i < Math.min(5, topApplicants.size()); i++) {
                java.util.Map.Entry<String, Integer> entry = topApplicants.get(i);
                String employeeName = employeeNames.get(entry.getKey());
                report.append(rank++).append(". ").append(employeeName)
                      .append(" - ").append(entry.getValue()).append(" requests\n");
            }
            report.append("\n");

            report.append("========================================\n");
            report.append("           END OF REPORT\n");
            report.append("========================================\n");

            // Determine file path
            String filename = "leave_report_" + year + ".txt";
            String filePath;
            if (outputPath == null || outputPath.trim().isEmpty()) {
                filePath = filename;
            } else {
                java.io.File dir = new java.io.File(outputPath);
                if (dir.isDirectory()) {
                    filePath = outputPath + java.io.File.separator + filename;
                } else {
                    filePath = outputPath;
                }
            }

            // Write to file
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                writer.write(report.toString());
            }

            return "Report generated successfully!\n" +
                   "File saved at: " + new java.io.File(filePath).getAbsolutePath() + "\n" +
                   "Total requests processed: " + totalRequests;

        } catch (Exception e) {
            System.out.println("Generate Report Error: " + e.getMessage());
            e.printStackTrace();
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Helper: Deduct leave balance from employee's Leave_Balance
     * Called when approving a leave request
     *
     * @param userId Employee's UID
     * @param leaveType Type of leave (annual, emergency, medical)
     * @param daysToDeduct Number of days to deduct
     * @return true if successful
     */
    private boolean deductLeaveBalance(String userId, String leaveType, int daysToDeduct) {
        try {
            // Get current leave balance document
            URL url = URI.create(FIRESTORE_URL + "/Leave_Balance").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");

                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String docUserId = getField(fields, "userid");
                        if (userId.equals(docUserId)) {
                            String leaveBalanceId = getField(fields, "leave_balance_id");
                            String year = getField(fields, "year");
                            int annualLeave = getIntField(fields, "annual_leave");
                            int emergencyLeave = getIntField(fields, "emergency_leave");
                            int medicalLeave = getIntField(fields, "medical_leave");

                            // Deduct from appropriate leave type
                            switch (leaveType.toLowerCase()) {
                                case "annual":
                                    annualLeave -= daysToDeduct;
                                    break;
                                case "emergency":
                                    emergencyLeave -= daysToDeduct;
                                    break;
                                case "medical":
                                    medicalLeave -= daysToDeduct;
                                    break;
                            }

                            // Update the leave balance (delete and recreate)
                            URL deleteUrl = URI.create(FIRESTORE_URL + "/Leave_Balance/" + leaveBalanceId).toURL();
                            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
                            deleteConn.setRequestMethod("DELETE");
                            deleteConn.getResponseCode();

                            URL createUrl = URI.create(FIRESTORE_URL + "/Leave_Balance?documentId=" + leaveBalanceId).toURL();
                            HttpURLConnection createConn = (HttpURLConnection) createUrl.openConnection();
                            createConn.setRequestMethod("POST");
                            createConn.setRequestProperty("Content-Type", "application/json");
                            createConn.setDoOutput(true);

                            JsonObject newFields = new JsonObject();
                            newFields.add("leave_balance_id", stringValue(leaveBalanceId));
                            newFields.add("userid", stringValue(userId));
                            newFields.add("year", stringValue(year));
                            newFields.add("annual_leave", integerValue(annualLeave));
                            newFields.add("emergency_leave", integerValue(emergencyLeave));
                            newFields.add("medical_leave", integerValue(medicalLeave));

                            JsonObject newDoc = new JsonObject();
                            newDoc.add("fields", newFields);

                            try (OutputStream os = createConn.getOutputStream()) {
                                os.write(newDoc.toString().getBytes(StandardCharsets.UTF_8));
                            }

                            int code = createConn.getResponseCode();
                            if (code == 200 || code == 201) {
                                System.out.println("Leave balance deducted: " + daysToDeduct + " days from " + leaveType);
                                return true;
                            }
                            return false;
                        }
                    }
                }
            }
            return false;

        } catch (Exception e) {
            System.out.println("Deduct Leave Balance Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all pending leave requests (HR only)
     * Filters Leave_Request collection for status = "Pending"
     *
     * @return Formatted string with all pending leave requests
     */
    public String getAllPendingLeaves() {
        try {
            URL url = URI.create(FIRESTORE_URL + "/Leave_Request").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                StringBuilder result = new StringBuilder();
                result.append("========================================\n");
                result.append("      PENDING LEAVE REQUESTS\n");
                result.append("========================================\n");

                if (json.has("documents")) {
                    JsonArray docs = json.getAsJsonArray("documents");
                    int count = 1;
                    for (JsonElement doc : docs) {
                        JsonObject docObj = doc.getAsJsonObject();
                        JsonObject fields = docObj.getAsJsonObject("fields");

                        String status = getField(fields, "status");
                        if (!"Pending".equalsIgnoreCase(status)) {
                            continue; // Skip non-pending requests
                        }

                        String leaveId = getField(fields, "leave_id");
                        String userId = getField(fields, "userid");
                        String employeeInfo = getEmployeeNameAndEmail(userId);
                        String leaveType = getField(fields, "leave_type");
                        String startDate = getField(fields, "start_date");
                        String endDate = getField(fields, "end_date");
                        int totalDays = getIntField(fields, "total_days");
                        String reason = getField(fields, "reason");
                        String dateCreated = getField(fields, "date_created_at");

                        result.append("\n[").append(count++).append("]\n");
                        result.append("Leave ID    : ").append(leaveId).append("\n");
                        result.append("Employee    : ").append(employeeInfo).append("\n");
                        result.append("Type        : ").append(capitalizeFirst(leaveType)).append("\n");
                        result.append("Period      : ").append(startDate).append(" to ").append(endDate).append("\n");
                        result.append("Total Days  : ").append(totalDays).append("\n");
                        result.append("Reason      : ").append(reason).append("\n");
                        result.append("Applied On  : ").append(dateCreated.substring(0, 10)).append("\n");
                        result.append("----------------------------------------");
                    }
                    if (count == 1) {
                        result.append("\nNo pending leave requests.\n");
                    }
                } else {
                    result.append("\nNo pending leave requests.\n");
                }
                result.append("\n========================================");
                return result.toString();
            }
            return "Failed to fetch pending leave requests.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Helper: Capitalize first letter of a string
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Helper: Get formatted status display with visual indicator
     */
    private String getStatusDisplay(String status) {
        if (status == null) {
            return "Unknown";
        }
        switch (status.toLowerCase()) {
            case "pending":
                return "⏳ Pending";
            case "approved":
                return "✅ Approved";
            case "rejected":
                return "❌ Rejected";
            default:
                return status;
        }
    }
}
