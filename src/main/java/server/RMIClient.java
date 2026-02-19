package server;

import hr.HRMenu;
import employee.EmployeeMenu;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * RMIClient - The RMI Client Application (Main Entry Point)
 *
 * This is the CLIENT side of the RMI system. Run this AFTER the server.
 *
 * What it does:
 * 1. Connects to the RMI Registry on localhost:1099
 * 2. Looks up the "AuthService" remote object
 * 3. Handles user login via Firebase Authenticationemad
 * 4. Redirects to appropriate menu based on user role (HR or Employee)
 *
 * The client calls methods on authService as if they were local,
 * but they actually execute on the server.
 */
public class RMIClient {

    private static AuthInterface authService; // Reference to the remote service

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Step 1: Connect to RMI Registry on the server (once at startup)
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthInterface) registry.lookup("AuthService");

            // Handshake: Notify server of connection
            authService.printConnection();

            // Main application loop - allows logout and login again
            boolean running = true;
            while (running) {
                System.out.println("========================================");
                System.out.println("        COMPANY LOGIN SYSTEM");
                System.out.println("========================================");
                System.out.println();

                // Step 2: Get login credentials from user
                System.out.println("Please login to continue");
                System.out.println("----------------------------------------");
                System.out.print("Email: ");
                String email = scanner.nextLine();
                System.out.print("Password: ");
                String password = scanner.nextLine();
                System.out.println("----------------------------------------");

                // Step 3: Authenticate with Firebase (via RMI call to server)
                String uid = authService.login(email, password);

                if (uid == null) {
                    System.out.println("Login Failed! Invalid email or password.");
                    System.out.println();
                    continue; // Go back to login prompt
                }

                // Step 4: Get user's role from Firestore (via RMI call)
                String role = authService.getRole(uid);

                if (role == null) {
                    System.out.println("Login Failed! Role not found.");
                    System.out.println();
                    continue; // Go back to login prompt
                }

                // Login successful - show user info
                System.out.println();
                System.out.println("========================================");
                System.out.println("         LOGIN SUCCESSFUL!");
                System.out.println("========================================");
                System.out.println("Email: " + email);
                System.out.println("UID: " + uid);
                System.out.println("Role: " + role);
                System.out.println("========================================");
                System.out.println();

                // Step 5: Redirect to appropriate menu based on role
                if ("hr".equalsIgnoreCase(role)) {
                    HRMenu.show(scanner, uid, email); // HR gets HR menu
                } else if ("employee".equalsIgnoreCase(role)) {
                    EmployeeMenu.show(scanner, uid, email); // Employee gets Employee menu
                } else {
                    System.out.println("Unknown role: " + role);
                }

                // After logout from menu, loop continues and shows login again
                System.out.println();
            }

        } catch (java.rmi.RemoteException | java.rmi.NotBoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
