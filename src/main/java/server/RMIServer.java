package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * RMIServer - The RMI Server Application
 *
 * This is the SERVER side of the RMI system. Run this FIRST before the client.
 *
 * What it does:
 * 1. Creates an RMI Registry on port 1099 (like a phone book for remote objects)
 * 2. Creates the Remote Object (AuthServiceImpl)
 * 3. Registers the object with a name "AuthService" so clients can find it
 *
 * The server keeps running and waits for client connections.
 */
public class RMIServer {
    public static void main(String[] args) {
        try {
            // Step 1: Create RMI Registry on port 1099
            // This is like creating a "directory" where remote objects are registered
            Registry registry = LocateRegistry.createRegistry(1099);

            // Step 2: Create the Remote Object (the actual service)
            AuthServiceImpl service = new AuthServiceImpl();

            // Step 3: Register the object with name "AuthService"
            // rebind() = register (or replace if already exists)
            // Clients will use this name to find the service
            registry.rebind("AuthService", service);

            System.out.println("========================================");
            System.out.println("         RMI SERVER STARTED");
            System.out.println("         Port: 1099");
            System.out.println("========================================");
            System.out.println("Waiting for connections...");

            // Server keeps running here, waiting for client requests

        } catch (java.rmi.RemoteException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }
}
