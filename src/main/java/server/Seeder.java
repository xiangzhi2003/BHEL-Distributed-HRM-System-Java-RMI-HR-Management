package server;

import database.AuthService;

/**
 * Seeder - Helper class to create a default user for testing
 * Run this to ensure the user exists in BOTH Firebase Auth and Firestore
 */
public class Seeder {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Running User Seeder...");
        System.out.println("========================================");

        AuthService authService = new AuthService();

        String email = "employee@bhel.com";
        String password = "password123";
        String firstName = "Test";
        String lastName = "Employee";
        String icPassport = "PASS123";
        String role = "employee";

        System.out.println("Attempting to create user: " + email);

        // This will create the user in Firebase Auth AND Firestore
        String result = authService.addEmployee(email, password, firstName, lastName, icPassport, role);

        System.out.println("Result: " + result);
        System.out.println("========================================");
        System.out.println("If the result says 'Email already exists', then the password might be different.");
        System.out.println("If it says 'Employee added successfully', you can now login.");
    }
}
