package database;

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
}
