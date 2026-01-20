package employee;

import server.AuthInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

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
     * @param scanner Shared Scanner for user input
     * @param uid Current user's UID (used to fetch their own data)
     * @param email Current user's email
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
                System.out.println("3. Logout");
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
                        running = false; // Exit the loop
                        System.out.println("\nLogged out. Goodbye!");
                        break;
                    default:
                        System.out.println("\nInvalid choice.\n");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * View own profile - Gets employee details from Firestore
     * @param uid Current user's UID
     */
    private static void viewProfile(String uid) {
        try {
            String profile = authService.getEmployeeByUid(uid); // RMI call
            System.out.println("\n" + profile + "\n");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * View own payroll history - Gets payroll entries from Firestore
     * @param uid Current user's UID
     */
    private static void viewMyPayroll(String uid) {
        try {
            String payroll = authService.getPayrollByUserId(uid); // RMI call
            System.out.println("\n" + payroll + "\n");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
