package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * RMIServer - The RMI Server Application
 *
 * This is the SERVER side of the RMI system. Run this FIRST before the client.
 *
 * What it does:
 * 1. Creates an RMI Registry on port 1099 (like a phone book for remote
 * objects)
 * 2. Creates the Remote Object (AuthServiceImpl)
 * 3. Registers the object with a name "AuthService" so clients can find it
 *
 * The server keeps running and waits for client connections.
 */
public class RMIServer {
    public static void main(String[] args) {
        // 1. Perform Network Time Verification (NTP)
        syncTime();

        // 2. Start SNMP Monitoring Agent
        startSnmpMonitor();

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
            System.out.println("Waiting for the connections... (Press Enter to stop)");
            new java.util.Scanner(System.in).nextLine();

            // Server keeps running here, waiting for client requests

        } catch (java.rmi.RemoteException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }

    // ==================== NTP IMPLEMENTATION ====================

    /**
     * Synchronize time with public NTP server
     * Checks for drift between local system time and atomic clock time
     */
    private static void syncTime() {
        String ntpServer = "time.google.com";
        System.out.println("\n[NTP] Connecting to " + ntpServer + "...");

        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(ntpServer);
            org.apache.commons.net.ntp.NTPUDPClient client = new org.apache.commons.net.ntp.NTPUDPClient();
            client.setDefaultTimeout(5000); // 5 seconds timeout

            org.apache.commons.net.ntp.TimeInfo info = client.getTime(address);
            long returnTime = info.getReturnTime(); // Local receiving time
            long serverTime = info.getMessage().getTransmitTimeStamp().getTime(); // Server sending time

            // Calculate drift
            long drift = serverTime - returnTime;

            System.out.println("[NTP] Connected successfully!");
            System.out.println("[NTP] Atomic Time   : " + new java.util.Date(serverTime));
            System.out.println("[NTP] System Time   : " + new java.util.Date(returnTime));
            System.out.println("[NTP] Time Drift    : " + drift + "ms");

            if (Math.abs(drift) > 1000) {
                System.out.println("[NTP] WARNING: System clock is desynchronized by >1 second!");
            } else {
                System.out.println("[NTP] System clock is synchronized.");
            }

        } catch (Exception e) {
            // Fallback for demo if internet/NTP port blocked: Simulate check
            System.out.println("[NTP] Verification failed (Network/Firewall issue): " + e.getMessage());
        }
    }

    // ==================== SNMP SIMULATION ====================

    /**
     * Start a background thread to simulate an SNMP Agent
     * Logos system metrics every 30 seconds
     */
    private static void startSnmpMonitor() {
        Thread snmpThread = new Thread(() -> {
            System.out.println("\n[SNMP] Agent started. Monitoring system metrics...");
            java.lang.management.MemoryMXBean memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            java.lang.management.RuntimeMXBean runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();

            while (true) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds

                    long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024); // MB
                    long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024); // MB
                    long uptime = runtimeBean.getUptime() / 1000; // Seconds

                    System.out.println("\n[SNMP] --- SYSTEM HEALTH REPORT ---");
                    System.out.println("[SNMP] Status      : OK");
                    System.out.println("[SNMP] Uptime      : " + uptime + "s");
                    System.out.println("[SNMP] Memory      : " + heapUsed + "MB / " + heapMax + "MB");
                    System.out.println("[SNMP] Threads     : " + Thread.activeCount());
                    System.out.println("[SNMP] ----------------------------");

                } catch (InterruptedException e) {
                    System.out.println("[SNMP] Agent stopped.");
                    break;
                }
            }
        });
        snmpThread.setDaemon(true); // Allow JVM to exit if main thread stops
        snmpThread.start();
    }
}
