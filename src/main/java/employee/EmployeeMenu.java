package employee;

import server.AuthInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * EmployeeMenu - Employee Dashboard User Interface
 *
 * This class provides the menu interface for regular employees.
 * Employees can only perform the following operations:
 * - View their own profile
 * - View their own payroll history
 *
 * Employees CANNOT modify any data (read-only access).
 * All operations are done via RMI calls to the server.
 */
public class EmployeeMenu {

    // Reference to the remote service (RMI stub)
    private static AuthInterface authService;

    /**
     * Main entry point for Employee menu
     * 
     * @param scanner Shared Scanner for user input
     * @param uid     Current user's UID (used to fetch their own data)
     * @param email   Current user's email
     */
    public static void show(Scanner scanner, String uid, String email) {
        try {
            // Connect to RMI service
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthInterface) registry.lookup("AuthService");

            System.out.println("========================================");
            System.out.println("         EMPLOYEE PORTAL");
            System.out.println("========================================");
            System.out.println("Welcome, Employee!");
            System.out.println();

            // Main menu loop - keeps running until user logs out
            boolean running = true;
            while (running) {
                // Display menu options (limited compared to HR)
                System.out.println("1. View My Profile");
                System.out.println("2. View My Payroll");
                System.out.println("3. Update My Profile");
                System.out.println("4. Logout");
                System.out.println("----------------------------------------");
                System.out.print("Choice: ");

                String choice = scanner.nextLine();

                // Handle user choice
                switch (choice) {
                    case "1":
                        viewProfile(uid); // Pass uid to view own profile
                        break;
                    case "2":
                        viewMyPayroll(uid); // Pass uid to view own payroll
                        break;
                    case "3":
                        updateProfile(uid, scanner);
                        break;
                    case "4":
                        running = false; // Exit the loop
                        System.out.println("\nLogged out. Goodbye!");
                        break;
                    default:
                        System.out.println("\nInvalid choice.\n");
                }
            }

        } catch (java.rmi.RemoteException | java.rmi.NotBoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * View own profile - Gets employee details from Firestore
     * 
     * @param uid Current user's UID
     */
    private static void viewProfile(String uid) {
        try {
            String profile = authService.getEmployeeByUid(uid); // RMI call
            System.out.println("\n" + profile + "\n");
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * View own payroll history - Gets payroll entries from Firestore
     * 
     * @param uid Current user's UID
     */
    private static void viewMyPayroll(String uid) {
        try {
            String payroll = authService.getPayrollByUserId(uid); // RMI call
            System.out.println("\n" + payroll + "\n");
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Update own profile - Prompts for new details to update
     * 
     * @param uid     Current user's UID
     * @param scanner Shared Scanner
     */
    /**
     * Update own profile - Prompts for new details to update
     * Supports "Leave blank to keep current value"
     * 
     * @param uid     Current user's UID
     * @param scanner Shared Scanner
     */
    private static void updateProfile(String uid, Scanner scanner) {
        try {
            // Step 1: Fetch current data to use as defaults
            String rawJson = authService.getEmployeeRaw(uid);
            String currentEmail = "";
            String currentFirst = "";
            String currentLast = "";
            String currentIc = "";

            if (rawJson != null) {
                JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
                JsonObject fields = json.getAsJsonObject("fields");

                if (fields.has("email"))
                    currentEmail = fields.getAsJsonObject("email").get("stringValue").getAsString();
                if (fields.has("first_name"))
                    currentFirst = fields.getAsJsonObject("first_name").get("stringValue").getAsString();
                if (fields.has("last_name"))
                    currentLast = fields.getAsJsonObject("last_name").get("stringValue").getAsString();
                if (fields.has("ic_passport"))
                    currentIc = fields.getAsJsonObject("ic_passport").get("stringValue").getAsString();
            }

            System.out.println("\n--- Update Profile ---");
            System.out.println("Instructions: Leave any field BLANK to keep the current value.");

            // Email
            System.out.println("\nCurrent Email: " + currentEmail);
            System.out.print("New Email (Enter to keep): ");
            String email = scanner.nextLine();
            if (email.trim().isEmpty()) {
                email = currentEmail;
            }

            // First Name
            System.out.println("Current First Name: " + currentFirst);
            System.out.print("New First Name (Enter to keep): ");
            String firstName = scanner.nextLine();
            if (firstName.trim().isEmpty()) {
                firstName = currentFirst;
            }

            // Last Name
            System.out.println("Current Last Name: " + currentLast);
            System.out.print("New Last Name (Enter to keep): ");
            String lastName = scanner.nextLine();
            if (lastName.trim().isEmpty()) {
                lastName = currentLast;
            }

            // IC/Passport
            System.out.println("Current IC/Passport: " + currentIc);
            System.out.print("New IC/Passport (Enter to keep): ");
            String icPassport = scanner.nextLine();
            if (icPassport.trim().isEmpty()) {
                icPassport = currentIc;
            }

            // Confirm update
            System.out.println("\nUpdating profile...");
            boolean success = authService.updateOwnProfile(uid, email, firstName, lastName, icPassport);

            if (success) {
                System.out.println("\nProfile updated successfully!");
                if (!email.equals(currentEmail)) {
                    System.out.println("NOTE: You changed your email. Please use the new email for next login.");
                }
                viewProfile(uid); // Show updated profile
            } else {
                System.out.println("\nFailed to update profile. Please try again.");
            }

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
