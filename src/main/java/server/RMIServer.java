package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            AuthServiceImpl service = new AuthServiceImpl();
            registry.rebind("AuthService", service);

            System.out.println("========================================");
            System.out.println("         RMI SERVER STARTED");
            System.out.println("         Port: 1099");
            System.out.println("========================================");
            System.out.println("Waiting for connections...");

        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
