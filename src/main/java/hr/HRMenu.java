package hr;

import server.AuthInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * HRMenu - HR Dashboard User Interface
 *
 * This class provides the menu interface for HR users.
 * HR can perform the following operations:
 * - View all employees
 * - Add new employee (creates Firebase Auth + Firestore record)
 * - Edit employee details
 * - Delete employee (removes from Auth + Firestore + Payroll)
 * - Manage payroll (CRUD operations on Payroll_Salary collection)
 *
 * All operations are done via RMI calls to the server.
 */
public class HRMenu {

    // Reference to the remote service (RMI stub)
    private static AuthInterface authService;

    /**
     * Main entry point for HR menu
     * @param scanner Shared Scanner for user input
     * @param uid Current user's UID
     * @param email Current user's email
     */
    public static void show(Scanner scanner, String uid, String email) {
        try {
            // Connect to RMI service (same as in RMIClient)
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthInterface) registry.lookup("AuthService");

            System.out.println("========================================");
            System.out.println("         HR MANAGEMENT SYSTEM");
            System.out.println("========================================");
            System.out.println("Welcome, HR!");
            System.out.println();

            // Main menu loop - keeps running until user logs out
            boolean running = true;
            while (running) {
                // Display menu options
                System.out.println("\n1. View All Employees");
                System.out.println("2. Add Employee");
                System.out.println("3. Edit Employee");
                System.out.println("4. Delete Employee");
                System.out.println("5. Manage Payroll");
                System.out.println("6. View Pending Leave Requests");
                System.out.println("7. Logout");
                System.out.println("----------------------------------------");
                System.out.print("Choice: ");

                String choice = scanner.nextLine();

                // Handle user choice
                switch (choice) {
                    case "1":
                        viewAllEmployees();
                        break;
                    case "2":
                        addEmployee(scanner);
                        break;
                    case "3":
                        editEmployee(scanner);
                        break;
                    case "4":
                        deleteEmployee(scanner);
                        break;
                    case "5":
                        managePayroll(scanner);
                        break;
                    case "6":
                        viewPendingLeaveRequests();
                        break;
                    case "7":
                        running = false; // Exit the loop
                        System.out.println("\nLogged out. Goodbye!");
                        break;
                    default:
                        System.out.println("\nInvalid choice.");
                }
            }

        } catch (java.rmi.RemoteException | java.rmi.NotBoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== EMPLOYEE CRUD METHODS ====================

    /**
     * View all employees - Calls server to get employee list from Firestore
     */
    private static void viewAllEmployees() {
        try {
            String result = authService.getAllEmployees(); // RMI call
            System.out.println("\n" + result);
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Add new employee - Creates user in Firebase Auth + Firestore
     */
    private static void addEmployee(Scanner scanner) {
        try {
            System.out.println("\n========================================");
            System.out.println("           ADD NEW EMPLOYEE");
            System.out.println("========================================");

            System.out.print("Email: ");
            String email = scanner.nextLine();

            // Validate email must be @gmail.com
            if (!email.toLowerCase().endsWith("@gmail.com")) {
                System.out.println("\nError: Only Gmail accounts are allowed. Please use @gmail.com");
                return;
            }

            System.out.print("Password: ");
            String password = scanner.nextLine();

            System.out.print("First Name: ");
            String firstName = scanner.nextLine();

            System.out.print("Last Name: ");
            String lastName = scanner.nextLine();

            System.out.print("IC/Passport Number: ");
            String icPassport = scanner.nextLine();

            // Role is fixed as "employee"
            String role = "employee";

            String result = authService.addEmployee(email, password, firstName, lastName, icPassport, role);
            System.out.println("\n" + result);

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Edit employee - Updates employee data in Firestore
     */
    private static void editEmployee(Scanner scanner) {
        try {
            System.out.println("\n========================================");
            System.out.println("           EDIT EMPLOYEE");
            System.out.println("========================================");

            // Show all employees first
            System.out.println("\nAvailable Employees:");
            String employees = authService.getAllEmployees();
            System.out.println(employees);

            System.out.print("\nEnter Employee UID: ");
            String uid = scanner.nextLine();

            // Show current details
            String details = authService.getEmployeeByUid(uid);
            System.out.println("\nCurrent Details:");
            System.out.println(details);

            if (details.contains("not found")) {
                return;
            }

            System.out.println("\nEnter new details:");
            System.out.print("First Name: ");
            String firstName = scanner.nextLine();

            System.out.print("Last Name: ");
            String lastName = scanner.nextLine();

            System.out.print("IC/Passport Number: ");
            String icPassport = scanner.nextLine();

            // Role stays as "employee"
            String role = "employee";

            boolean success = authService.updateEmployee(uid, firstName, lastName, icPassport, role);

            if (success) {
                System.out.println("\nEmployee updated successfully!");
            } else {
                System.out.println("\nFailed to update employee.");
            }

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Delete employee - Removes from Firebase Auth + Firestore + Payroll
     */
    private static void deleteEmployee(Scanner scanner) {
        try {
            System.out.println("\n========================================");
            System.out.println("          DELETE EMPLOYEE");
            System.out.println("========================================");

            // Show all employees first
            System.out.println("\nAvailable Employees:");
            String employees = authService.getAllEmployees();
            System.out.println(employees);

            System.out.print("\nEnter Employee UID: ");
            String uid = scanner.nextLine();

            // Show current details
            String details = authService.getEmployeeByUid(uid);
            System.out.println("\nEmployee Details:");
            System.out.println(details);

            if (details.contains("not found")) {
                return;
            }

            System.out.print("\nAre you sure you want to delete? (yes/no): ");
            String confirm = scanner.nextLine();

            if ("yes".equalsIgnoreCase(confirm)) {
                boolean success = authService.deleteEmployee(uid);

                if (success) {
                    System.out.println("\nEmployee deleted successfully!");
                } else {
                    System.out.println("\nFailed to delete employee.");
                }
            } else {
                System.out.println("\nDelete cancelled.");
            }

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== PAYROLL MANAGEMENT ====================
    // These methods handle CRUD operations for the Payroll_Salary collection

    /**
     * Payroll management entry point
     * First select an employee, then manage their payroll entries
     */
    private static void managePayroll(Scanner scanner) {
        boolean running = true;
        while (running) {
            try {
                System.out.println("\n========================================");
                System.out.println("         PAYROLL MANAGEMENT");
                System.out.println("========================================");
                System.out.println("Select an employee to manage their payroll:\n");

                // Show all employees
                String employees = authService.getAllEmployees();
                System.out.println(employees);

                System.out.println("\nEnter Employee UID (or 'back' to return): ");
                System.out.print("UID: ");
                String userId = scanner.nextLine();

                if ("back".equalsIgnoreCase(userId)) {
                    running = false;
                    continue;
                }

                // Verify employee exists and get their info
                String employeeDetails = authService.getEmployeeByUid(userId);
                if (employeeDetails.contains("not found")) {
                    System.out.println("\nEmployee not found. Please enter a valid UID.");
                    continue;
                }

                // Extract employee name and email for display
                String employeeName = extractEmployeeInfo(employeeDetails);

                // Show employee-specific payroll submenu
                manageEmployeePayroll(scanner, userId, employeeName);

            } catch (java.rmi.RemoteException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static String extractEmployeeInfo(String details) {
        // Parse employee details to extract name and email
        String firstName = "";
        String lastName = "";
        String email = "";

        String[] lines = details.split("\n");
        for (String line : lines) {
            if (line.contains("Email")) {
                email = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.contains("First Name")) {
                firstName = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.contains("Last Name")) {
                lastName = line.substring(line.indexOf(":") + 1).trim();
            }
        }

        return firstName + " " + lastName + " (" + email + ")";
    }

    private static void manageEmployeePayroll(Scanner scanner, String userId, String employeeName) {
        boolean running = true;
        while (running) {
            System.out.println("\n========================================");
            System.out.println("   PAYROLL FOR: " + employeeName);
            System.out.println("========================================");
            System.out.println("1. View Payroll History");
            System.out.println("2. Add Payroll Entry");
            System.out.println("3. Edit Payroll Entry");
            System.out.println("4. Delete Payroll Entry");
            System.out.println("5. Back to Employee Selection");
            System.out.println("----------------------------------------");
            System.out.print("Choice: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    viewEmployeePayroll(userId);
                    break;
                case "2":
                    addEmployeePayroll(scanner, userId);
                    break;
                case "3":
                    editEmployeePayroll(scanner, userId);
                    break;
                case "4":
                    deleteEmployeePayroll(scanner, userId);
                    break;
                case "5":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid choice.");
            }
        }
    }

    private static void viewEmployeePayroll(String userId) {
        try {
            String result = authService.getPayrollByUserId(userId);
            System.out.println("\n" + result);
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addEmployeePayroll(Scanner scanner, String userId) {
        try {
            System.out.println("\n========================================");
            System.out.println("         ADD PAYROLL ENTRY");
            System.out.println("========================================");

            System.out.print("Salary (RM): ");
            String salaryStr = scanner.nextLine();
            double salary;
            try {
                salary = Double.parseDouble(salaryStr);
                if (salary < 0) {
                    System.out.println("\nSalary cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid salary format.");
                return;
            }

            System.out.print("Month (01-12): ");
            String monthEntry = scanner.nextLine();

            System.out.print("Year (e.g., 2024): ");
            String yearEntry = scanner.nextLine();

            String result = authService.addPayroll(userId, salary, monthEntry, yearEntry);
            System.out.println("\n" + result);

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void editEmployeePayroll(Scanner scanner, String userId) {
        try {
            System.out.println("\n========================================");
            System.out.println("         EDIT PAYROLL ENTRY");
            System.out.println("========================================");

            // Show this employee's payroll entries
            String payrollHistory = authService.getPayrollByUserId(userId);
            System.out.println(payrollHistory);

            if (payrollHistory.contains("No payroll entries found")) {
                return;
            }

            System.out.print("\nEnter Payroll ID: ");
            String payrollId = scanner.nextLine();

            System.out.print("New Salary (RM): ");
            String salaryStr = scanner.nextLine();
            double salary;
            try {
                salary = Double.parseDouble(salaryStr);
                if (salary < 0) {
                    System.out.println("\nSalary cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid salary format.");
                return;
            }

            System.out.print("New Month (01-12): ");
            String monthEntry = scanner.nextLine();

            System.out.print("New Year (e.g., 2024): ");
            String yearEntry = scanner.nextLine();

            boolean success = authService.updatePayroll(payrollId, salary, monthEntry, yearEntry);

            if (success) {
                System.out.println("\nPayroll entry updated successfully!");
            } else {
                System.out.println("\nFailed to update payroll entry. Please check the Payroll ID, month, or year.");
            }

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void deleteEmployeePayroll(Scanner scanner, String userId) {
        try {
            System.out.println("\n========================================");
            System.out.println("        DELETE PAYROLL ENTRY");
            System.out.println("========================================");

            // Show this employee's payroll entries
            String payrollHistory = authService.getPayrollByUserId(userId);
            System.out.println(payrollHistory);

            if (payrollHistory.contains("No payroll entries found")) {
                return;
            }

            System.out.print("\nEnter Payroll ID: ");
            String payrollId = scanner.nextLine();

            System.out.print("Are you sure you want to delete? (yes/no): ");
            String confirm = scanner.nextLine();

            if ("yes".equalsIgnoreCase(confirm)) {
                boolean success = authService.deletePayroll(payrollId);

                if (success) {
                    System.out.println("\nPayroll entry deleted successfully!");
                } else {
                    System.out.println("\nFailed to delete payroll entry. Please check the Payroll ID.");
                }
            } else {
                System.out.println("\nDelete cancelled.");
            }

        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== LEAVE MANAGEMENT ====================

    /**
     * View all pending leave requests
     */
    private static void viewPendingLeaveRequests() {
        try {
            String result = authService.getAllPendingLeaves();
            System.out.println("\n" + result);
        } catch (java.rmi.RemoteException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
