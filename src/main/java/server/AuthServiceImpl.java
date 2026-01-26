package server;

import database.AuthService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * AuthServiceImpl - Remote Object Implementation
 *
 * This class implements the AuthInterface and provides the actual
 * functionality.
 * - Extends UnicastRemoteObject to make it a remote object (can be called
 * remotely)
 * - Implements AuthInterface (the contract)
 * - Acts as a BRIDGE between RMI calls and the actual business logic
 * (AuthService)
 *
 * Flow: Client -> AuthInterface -> AuthServiceImpl -> AuthService -> Firebase
 */
public class AuthServiceImpl extends UnicastRemoteObject implements AuthInterface {

    // The actual business logic class that handles Firebase operations
    private final AuthService authService;

    /**
     * Constructor - must throw RemoteException
     * super() exports this object so it can receive remote calls
     */
    public AuthServiceImpl() throws RemoteException {
        super(); // Export this object for remote access
        authService = new AuthService(); // Create instance of business logic
    }

    // ==================== AUTHENTICATION METHODS ====================
    // These methods delegate to AuthService which handles Firebase Auth

    @Override
    public String login(String email, String password) throws RemoteException {
        System.out.println("Server: Login request for " + email);
        return authService.login(email, password); // Delegates to AuthService
    }

    @Override
    public String getRole(String uid) throws RemoteException {
        System.out.println("Server: Getting role for UID " + uid);
        return authService.getRole(uid); // Gets role from Firestore
    }

    // ==================== EMPLOYEE CRUD METHODS ====================
    // These methods delegate to AuthService which handles Firestore operations

    @Override
    public String getAllEmployees() throws RemoteException {
        System.out.println("Server: Getting all employees");
        return authService.getAllEmployees();
    }

    @Override
    public String addEmployee(String email, String password, String firstName, String lastName, String icPassport,
            String role) throws RemoteException {
        System.out.println("Server: Adding employee - " + email);
        return authService.addEmployee(email, password, firstName, lastName, icPassport, role);
    }

    @Override
    public String getEmployeeByUid(String uid) throws RemoteException {
        System.out.println("Server: Getting employee - " + uid);
        return authService.getEmployeeByUid(uid);
    }

    @Override
    public boolean updateEmployee(String uid, String firstName, String lastName, String icPassport, String role)
            throws RemoteException {
        System.out.println("Server: Updating employee - " + uid);
        return authService.updateEmployee(uid, firstName, lastName, icPassport, role);
    }

    @Override
    public boolean updateOwnProfile(String uid, String email, String firstName, String lastName, String icPassport)
            throws RemoteException {
        System.out.println("Server: Updating own profile - " + uid);
        return authService.updateOwnProfile(uid, email, firstName, lastName, icPassport);
    }

    @Override
    public String getEmployeeRaw(String uid) throws RemoteException {
        return authService.getEmployeeRaw(uid);
    }

    @Override
    public boolean deleteEmployee(String uid) throws RemoteException {
        System.out.println("Server: Deleting employee - " + uid);
        return authService.deleteEmployee(uid);
    }

    // ==================== PAYROLL CRUD METHODS ====================
    // These methods delegate to AuthService which handles Payroll_Salary collection

    @Override
    public String getAllPayroll() throws RemoteException {
        System.out.println("Server: Getting all payroll entries");
        return authService.getAllPayroll();
    }

    @Override
    public String getPayrollByUserId(String userId) throws RemoteException {
        System.out.println("Server: Getting payroll for user - " + userId);
        return authService.getPayrollByUserId(userId);
    }

    @Override
    public String addPayroll(String userId, double salary, String monthEntry, String yearEntry) throws RemoteException {
        System.out.println("Server: Adding payroll for user - " + userId);
        return authService.addPayroll(userId, salary, monthEntry, yearEntry);
    }

    @Override
    public boolean updatePayroll(String payrollId, double salary, String monthEntry, String yearEntry)
            throws RemoteException {
        System.out.println("Server: Updating payroll - " + payrollId);
        return authService.updatePayroll(payrollId, salary, monthEntry, yearEntry);
    }

    @Override
    public boolean deletePayroll(String payrollId) throws RemoteException {
        System.out.println("Server: Deleting payroll - " + payrollId);
        return authService.deletePayroll(payrollId);
    }

    // ==================== LEAVE MANAGEMENT METHODS ====================
    // These methods delegate to AuthService which handles Leaves collection

    @Override
    public String applyLeave(String userId, String leaveType, String startDate, String endDate, int totalDays,
            String reason) throws RemoteException {
        System.out.println("Server: Applying leave for user - " + userId);
        return authService.applyLeave(userId, leaveType, startDate, endDate, totalDays, reason);
    }

    @Override
    public String getLeavesByUserId(String userId) throws RemoteException {
        System.out.println("Server: Getting leaves for user - " + userId);
        return authService.getLeavesByUserId(userId);
    }

    @Override
    public String getLeaveBalance(String userId) throws RemoteException {
        System.out.println("Server: Getting leave balance for user - " + userId);
        return authService.getLeaveBalance(userId);
    }

    @Override
    public java.util.Map<String, Integer> getLeaveBalanceData(String userId) throws RemoteException {
        System.out.println("Server: Getting leave balance data for user - " + userId);
        return authService.getLeaveBalanceData(userId);
    }

    @Override
    public boolean checkAndResetLeaveBalance(String userId) throws RemoteException {
        System.out.println("Server: Checking/resetting leave balance for user - " + userId);
        return authService.checkAndResetLeaveBalance(userId);
    }

    // ==================== HR LEAVE MANAGEMENT ====================

    @Override
    public String getAllPendingLeaves() throws RemoteException {
        System.out.println("Server: Getting all pending leave requests");
        return authService.getAllPendingLeaves();
    }
}
