package server;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.File;

public class TestAdminSdk {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("DIAGNOSTIC: Testing Firebase Admin SDK");
        System.out.println("Current Directory: " + new File(".").getAbsolutePath());
        System.out.println("========================================");

        String path = "serviceAccountKey.json";
        String[] pathsToCheck = {
                path,
                "../" + path,
                "BHEL-Distributed-HRM-System-Java-RMI-HR-Management/" + path
        };

        File foundFile = null;
        for (String p : pathsToCheck) {
            File f = new File(p);
            System.out.println("Checking: " + f.getAbsolutePath() + " -> " + (f.exists() ? "FOUND" : "MISSING"));
            if (f.exists())
                foundFile = f;
        }

        if (foundFile == null) {
            System.out.println("\n[FAIL] serviceAccountKey.json NOT FOUND!");
            System.out.println("Please place it in: " + new File(".").getAbsolutePath());
            return;
        }

        System.out.println("\n[SUCCESS] Found key at: " + foundFile.getAbsolutePath());

        try {
            FileInputStream serviceAccount = new FileInputStream(foundFile);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("distributed-system-data")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("[SUCCESS] FirebaseApp.initializeApp() passed.");
            } else {
                System.out.println("[INFO] FirebaseApp already initialized.");
            }

            System.out.println("\nDIAGNOSTIC PASSED: Admin SDK is ready.");
            System.out.println("If this script works but the Server fails, you MUST restart the RMIServer.");

        } catch (Exception e) {
            System.out.println("\n[FAIL] Exception initializing SDK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
