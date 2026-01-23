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
 * 3. Handles user login via Firebase Authentication
 * 4. Redirects to appropriate menu based on user role (HR or Employee)
 *
 * The client calls methods on authService as if they were local,
 * but they actually execute on the server.
 */
public class RMIClient {

    private static AuthInterface authService; // Reference to the remote service

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("========================================");
            System.out.println("        COMPANY LOGIN SYSTEM");
            System.out.println("========================================");
            System.out.println();

            // Step 1: Connect to RMI Registry on the server
            // getRegistry() connects to an existing registry (created by server)
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // Step 2: Look up the remote object by name "AuthService"
            // This returns a stub (proxy) that forwards calls to the server
            authService = (AuthInterface) registry.lookup("AuthService");

            // Step 3: Get login credentials from user
            System.out.println("Please login to continue");
            System.out.println("----------------------------------------");
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            System.out.println("----------------------------------------");

            // Step 4: Authenticate with Firebase (via RMI call to server)
            // This call goes: Client -> RMI -> Server -> Firebase
            String uid = authService.login(email, password);

            if (uid == null) {
                System.out.println("Login Failed! Invalid email or password.");
                return;
            }

            // Step 5: Get user's role from Firestore (via RMI call)
            String role = authService.getRole(uid);

            if (role == null) {
                System.out.println("Login Failed! Role not found.");
                return;
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

            // Step 6: Redirect to appropriate menu based on role
            if ("hr".equalsIgnoreCase(role)) {
                HRMenu.show(scanner, uid, email);  // HR gets HR menu
            } else if ("employee".equalsIgnoreCase(role)) {
                EmployeeMenu.show(scanner, uid, email);  // Employee gets Employee menu
            } else {
                System.out.println("Unknown role: " + role);
            }

        } catch (java.rmi.RemoteException | java.rmi.NotBoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
