package server;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RMIServer - BHEL Distributed HRM System
 *
 * Run this FIRST before the client.
 *
 * Startup sequence:
 *  1. NTP clock verification
 *  2. SNMP health monitor (background thread)
 *  3. RMI Registry + AuthService binding
 *  4. Graceful shutdown hook (Ctrl+C)
 */
public class RMIServer {

    private static final int    PORT         = 1099;
    private static final String SERVICE_NAME = "AuthService";
    private static final String NTP_SERVER   = "time.google.com";

    public static void main(String[] args) {
        syncTime();
        startSnmpMonitor();

        try {
            Registry registry = LocateRegistry.createRegistry(PORT);
            AuthServiceImpl service = new AuthServiceImpl();
            registry.rebind(SERVICE_NAME, service);

            // Graceful shutdown on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SERVER] Shutting down...");
                try {
                    registry.unbind(SERVICE_NAME);
                    UnicastRemoteObject.unexportObject(registry, true);
                    System.out.println("[SERVER] Stopped cleanly.");
                } catch (Exception e) {
                    System.out.println("[SERVER] Shutdown warning: could not cleanly unbind service");
                }
            }));

            printBanner();
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join(); // keep alive

        } catch (java.rmi.RemoteException e) {
            System.out.println("[SERVER] Failed to start - ensure port " + PORT + " is not already in use");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== STARTUP BANNER ====================

    private static void printBanner() {
        String host = "localhost";
        String ip   = "127.0.0.1";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = addr.getHostName();
            ip   = addr.getHostAddress();
        } catch (Exception ignored) {}

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println();
        System.out.println("==========================================");
        System.out.println("   BHEL HRM SYSTEM  —  RMI SERVER");
        System.out.println("==========================================");
        System.out.printf("   Host    : %s (%s)%n", host, ip);
        System.out.printf("   Port    : %d%n", PORT);
        System.out.printf("   Service : %s%n", SERVICE_NAME);
        System.out.printf("   Started : %s%n", time);
        System.out.println("==========================================");
    }

    // ==================== NTP TIME VERIFICATION ====================

    private static void syncTime() {
        System.out.println("[NTP] Connecting to " + NTP_SERVER + "...");
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(NTP_SERVER);
            org.apache.commons.net.ntp.NTPUDPClient client = new org.apache.commons.net.ntp.NTPUDPClient();
            client.setDefaultTimeout(5000);

            org.apache.commons.net.ntp.TimeInfo info = client.getTime(address);
            long serverTime = info.getMessage().getTransmitTimeStamp().getTime();
            long localTime  = info.getReturnTime();
            long drift      = serverTime - localTime;

            System.out.println("[NTP] Atomic time  : " + new java.util.Date(serverTime));
            System.out.println("[NTP] System time  : " + new java.util.Date(localTime));
            System.out.printf("[NTP] Clock drift  : %d ms%n", drift);

            if (Math.abs(drift) > 1000) {
                System.out.println("[NTP] WARNING: Clock drift > 1 second — consider syncing.");
            } else {
                System.out.println("[NTP] System clock is synchronised.");
            }
            client.close();

        } catch (Exception e) {
            System.out.println("[NTP] Skipped (could not reach " + NTP_SERVER + ")");
        }
    }

    // ==================== SNMP HEALTH MONITOR ====================

    private static void startSnmpMonitor() {
        java.lang.management.MemoryMXBean  mem     = java.lang.management.ManagementFactory.getMemoryMXBean();
        java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();

        Thread monitor = new Thread(() -> {
            System.out.println("[SNMP] Health monitor started (every 30s).");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30_000);
                    long heapMB  = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
                    long maxMB   = mem.getHeapMemoryUsage().getMax()  / (1024 * 1024);
                    long uptime  = runtime.getUptime() / 1000;
                    System.out.printf("[SNMP] uptime=%ds  heap=%d/%dMB  threads=%d%n",
                            uptime, heapMB, maxMB, Thread.activeCount());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "snmp-monitor");

        monitor.setDaemon(true);
        monitor.start();
    }
}
