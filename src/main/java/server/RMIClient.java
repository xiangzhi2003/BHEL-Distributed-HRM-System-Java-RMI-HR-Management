package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class RMIClient {

    private static Scanner scanner = new Scanner(System.in);
    private static AuthInterface authService;

    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("        COMPANY LOGIN SYSTEM");
            System.out.println("========================================");
            System.out.println();

            // Connect to RMI server
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthInterface) registry.lookup("AuthService");

            // Show login
            System.out.println("Please login to continue");
            System.out.println("----------------------------------------");
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            System.out.println("----------------------------------------");

            // Authenticate with Firebase
            String uid = authService.login(email, password);

            if (uid == null) {
                System.out.println("Login Failed! Invalid email or password.");
                return;
            }

            // Login successful
            System.out.println();
            System.out.println("========================================");
            System.out.println("         LOGIN SUCCESSFUL!");
            System.out.println("========================================");
            System.out.println("Email: " + email);
            System.out.println("UID: " + uid);
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
