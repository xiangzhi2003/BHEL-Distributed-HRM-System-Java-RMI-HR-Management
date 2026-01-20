package employee;

import server.AuthInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class EmployeeMenu {

    private static AuthInterface authService;

    public static void show(Scanner scanner, String uid, String email) {
        try {
            // Get RMI service
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthInterface) registry.lookup("AuthService");

            System.out.println("========================================");
            System.out.println("         EMPLOYEE PORTAL");
            System.out.println("========================================");
            System.out.println("Welcome, Employee!");
            System.out.println();

            boolean running = true;
            while (running) {
                System.out.println("1. View My Profile");
                System.out.println("2. Logout");
                System.out.println("----------------------------------------");
                System.out.print("Choice: ");

                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        viewProfile(uid);
                        break;
                    case "2":
                        running = false;
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

    private static void viewProfile(String uid) {
        try {
            String profile = authService.getEmployeeByUid(uid);
            System.out.println("\n" + profile + "\n");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
