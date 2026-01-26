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
                System.out.println("4. Apply for Leave");
                System.out.println("5. Logout");
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
                        manageLeave(scanner, uid); // New leave management feature
                        break;
                    case "5":
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

    // ==================== LEAVE MANAGEMENT ====================

    /**
     * Leave management submenu for employees
     * Options: Apply for new leave, View leave history, View leave balance
     * Automatically checks and resets leave balance if new year
     *
     * @param scanner Shared Scanner
     * @param uid     Current user's UID
     */
    private static void manageLeave(Scanner scanner, String uid) {
        boolean running = true;

        // Check and reset leave balance on entry (handles new year reset)
        try {
            authService.checkAndResetLeaveBalance(uid);
        } catch (java.rmi.RemoteException e) {
            System.out.println("Warning: Could not verify leave balance - " + e.getMessage());
        }

        while (running) {
            try {
                System.out.println("\n========================================");
                System.out.println("           LEAVE MANAGEMENT");
                System.out.println("========================================");
                System.out.println("1. Apply for Leave");
                System.out.println("2. View My Leave History");
                System.out.println("3. View My Leave Balance");
                System.out.println("4. Back to Main Menu");
                System.out.println("----------------------------------------");
                System.out.print("Choice: ");

                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        applyForLeave(scanner, uid);
                        break;
                    case "2":
                        viewLeaveHistory(uid);
                        break;
                    case "3":
                        viewLeaveBalance(uid);
                        break;
                    case "4":
                        running = false;
                        break;
                    default:
                        System.out.println("\nInvalid choice.\n");
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Apply for a new leave - Prompts user for leave details
     * Validates all inputs before submission
     *
     * @param scanner Shared Scanner
     * @param uid     Current user's UID
     */
    private static void applyForLeave(Scanner scanner, String uid) {
        try {
            System.out.println("\n========================================");
            System.out.println("           APPLY FOR LEAVE");
            System.out.println("========================================");

            // Get leave balance data
            java.util.Map<String, Integer> balanceData = authService.getLeaveBalanceData(uid);
            int annualBalance = balanceData.getOrDefault("annual", 0);
            int emergencyBalance = balanceData.getOrDefault("emergency", 0);
            int medicalBalance = balanceData.getOrDefault("medical", 0);

            // Step 1: Select leave type (with balance display)
            System.out.println("\nSelect Leave Type:");
            System.out.println("1. Annual Leave (" + annualBalance + "/10)");
            System.out.println("2. Emergency Leave (" + emergencyBalance + "/10)");
            System.out.println("3. Medical Leave (" + medicalBalance + "/10)");
            System.out.println("----------------------------------------");
            System.out.print("Choice (1-3): ");
            String typeChoice = scanner.nextLine();

            String leaveType;
            switch (typeChoice) {
                case "1":
                    leaveType = "annual";
                    break;
                case "2":
                    leaveType = "emergency";
                    break;
                case "3":
                    leaveType = "medical";
                    break;
                default:
                    System.out.println("\nInvalid leave type. Please try again.");
                    return;
            }

            // Step 2: Get start date
            System.out.println("\nEnter Start Date. Please use YYYY-MM-DD (e.g., 2026-02-05)");
            System.out.print("Start Date: ");
            String startDate = scanner.nextLine().trim();

            // Validate start date format
            if (!isValidDateFormat(startDate)) {
                System.out.println("\nInvalid date format. Please use YYYY-MM-DD (e.g., 2026-02-05)");
                return;
            }

            // Step 3: Get end date
            System.out.println("\nEnter End Date (format: YYYY-MM-DD)");
            System.out.print("End Date: ");
            String endDate = scanner.nextLine().trim();

            // Validate end date format
            if (!isValidDateFormat(endDate)) {
                System.out.println("\nInvalid date format. Please use YYYY-MM-DD (e.g., 2026-02-20)");
                return;
            }

            // Step 4: Calculate total days automatically
            int calculatedDays = calculateDaysBetween(startDate, endDate);
            if (calculatedDays < 0) {
                System.out.println("\nEnd date cannot be before start date.");
                return;
            }
            if (calculatedDays == 0) {
                calculatedDays = 1; // Same day leave is 1 day
            } else {
                calculatedDays = calculatedDays + 1; // Include both start and end dates
            }

            int totalDays = calculatedDays;

            // Step 5: Get reason
            System.out.println("\nEnter Reason for Leave:");
            System.out.print("Reason: ");
            String reason = scanner.nextLine().trim();

            if (reason.isEmpty()) {
                System.out.println("\nReason is required.");
                return;
            }

            // Step 6: Submit via RMI call (status will be automatically set to "Pending")
            String result = authService.applyLeave(uid, leaveType, startDate, endDate, totalDays, reason);
            System.out.println("\n" + result);

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * View leave history for the current employee
     *
     * @param uid Current user's UID
     */
    private static void viewLeaveHistory(String uid) {
        try {
            String history = authService.getLeavesByUserId(uid);
            System.out.println("\n" + history);
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * View leave balance for the current employee
     * Shows remaining days for each leave type
     *
     * @param uid Current user's UID
     */
    private static void viewLeaveBalance(String uid) {
        try {
            String balance = authService.getLeaveBalance(uid);
            System.out.println("\n" + balance);
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS FOR LEAVE ====================

    /**
     * Validate date format (YYYY-MM-DD)
     *
     * @param date Date string to validate
     * @return true if valid format
     */
    private static boolean isValidDateFormat(String date) {
        if (date == null || date.length() != 10) {
            return false;
        }
        try {
            // Check format matches YYYY-MM-DD
            if (date.charAt(4) != '-' || date.charAt(7) != '-') {
                return false;
            }
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8, 10));

            // Basic validation
            if (year < 2000 || year > 2100)
                return false;
            if (month < 1 || month > 12)
                return false;
            if (day < 1 || day > 31)
                return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Calculate days between two dates
     *
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate   End date (YYYY-MM-DD)
     * @return Number of days between dates (negative if end is before start)
     */
    private static int calculateDaysBetween(String startDate, String endDate) {
        try {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Capitalize first letter of a string
     *
     * @param str Input string
     * @return Capitalized string
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}

