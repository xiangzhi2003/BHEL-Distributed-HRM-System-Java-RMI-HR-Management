package hr;

import server.AuthInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class HRMenu {

    private static AuthInterface authService;

    public static void show(Scanner scanner, String uid, String email) {
        try {
            // Get RMI service
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthInterface) registry.lookup("AuthService");

            System.out.println("========================================");
            System.out.println("         HR MANAGEMENT SYSTEM");
            System.out.println("========================================");
            System.out.println("Welcome, HR!");
            System.out.println();

            boolean running = true;
            while (running) {
                System.out.println("\n1. View All Employees");
                System.out.println("2. Add Employee");
                System.out.println("3. Edit Employee");
                System.out.println("4. Delete Employee");
                System.out.println("5. Logout");
                System.out.println("----------------------------------------");
                System.out.print("Choice: ");

                String choice = scanner.nextLine();

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
                        running = false;
                        System.out.println("\nLogged out. Goodbye!");
                        break;
                    default:
                        System.out.println("\nInvalid choice.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewAllEmployees() {
        try {
            String result = authService.getAllEmployees();
            System.out.println("\n" + result);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addEmployee(Scanner scanner) {
        try {
            System.out.println("\n========================================");
            System.out.println("           ADD NEW EMPLOYEE");
            System.out.println("========================================");

            System.out.print("Email: ");
            String email = scanner.nextLine();

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

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

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

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

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

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
