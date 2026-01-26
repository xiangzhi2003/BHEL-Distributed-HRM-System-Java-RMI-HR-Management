package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * AuthInterface - Remote Interface for RMI
 *
 * This is the CONTRACT between client and server in RMI.
 * - Must extend java.rmi.Remote
 * - All methods must throw RemoteException
 * - Both client and server must have access to this interface
 *
 * The client uses this interface to call methods on the server remotely.
 * The server implements this interface to provide the actual functionality.
 */
public interface AuthInterface extends Remote {

        // ==================== AUTHENTICATION ====================

        /**
         * Authenticate user with email and password
         * 
         * @param email    User's email address
         * @param password User's password
         * @return User's UID if login successful, null if failed
         */
        String login(String email, String password) throws RemoteException;

        /**
         * Get user's role from database
         * 
         * @param uid User's unique ID
         * @return Role string ("hr" or "employee"), null if not found
         */
        String getRole(String uid) throws RemoteException;

        // ==================== EMPLOYEE CRUD OPERATIONS ====================

        /**
         * Get list of all employees (HR only)
         * 
         * @return Formatted string containing all employee data
         */
        String getAllEmployees() throws RemoteException;

        /**
         * Register a new employee in the system
         * 
         * @return Success/error message
         */
        String addEmployee(String email, String password, String firstName, String lastName, String icPassport,
                        String role)
                        throws RemoteException;

        /**
         * Get single employee details by UID
         * 
         * @param uid Employee's unique ID
         * @return Formatted string with employee details
         */
        String getEmployeeByUid(String uid) throws RemoteException;

        /**
         * Update employee information
         * 
         * @return true if successful, false if failed
         */
        boolean updateEmployee(String uid, String firstName, String lastName, String icPassport, String role)
                        throws RemoteException;

        /**
         * Update own profile information (Employee only)
         * Cannot change role
         * 
         * @return true if successful, false if failed
         */
        boolean updateOwnProfile(String uid, String email, String firstName, String lastName, String icPassport)
                        throws RemoteException;

        /**
         * Get raw employee data (JSON format)
         * Used for client-side parsing to pre-fill forms
         */
        String getEmployeeRaw(String uid) throws RemoteException;

        /**
         * Delete employee from system (removes from Auth + Firestore + Payroll)
         * 
         * @param uid Employee's unique ID
         * @return true if successful, false if failed
         */
        boolean deleteEmployee(String uid) throws RemoteException;

        // ==================== PAYROLL CRUD OPERATIONS ====================

        /**
         * Get all payroll entries (HR only)
         * 
         * @return Formatted string with all payroll data
         */
        String getAllPayroll() throws RemoteException;

        /**
         * Get payroll history for a specific employee
         * 
         * @param userId Employee's UID
         * @return Formatted string with payroll history
         */
        String getPayrollByUserId(String userId) throws RemoteException;

        /**
         * Add new payroll entry for an employee
         * 
         * @return Success/error message
         */
        String addPayroll(String userId, double salary, String monthEntry, String yearEntry) throws RemoteException;

        /**
         * Update existing payroll entry
         * 
         * @return true if successful, false if failed
         */
        boolean updatePayroll(String payrollId, double salary, String monthEntry, String yearEntry)
                        throws RemoteException;

        /**
         * Delete payroll entry
         * 
         * @param payrollId Payroll entry ID
         * @return true if successful, false if failed
         */
        boolean deletePayroll(String payrollId) throws RemoteException;

        // ==================== LEAVE MANAGEMENT OPERATIONS ====================

        /**
         * Apply for leave (Employee only)
         * Creates a new leave request with status "Pending"
         *
         * @param userId    Employee's UID
         * @param leaveType Type of leave ("annual" | "emergency" | "medical")
         * @param startDate Start date of leave (format: YYYY-MM-DD)
         * @param endDate   End date of leave (format: YYYY-MM-DD)
         * @param totalDays Total number of leave days
         * @param reason    Reason for leave
         * @return Success/error message
         */
        String applyLeave(String userId, String leaveType, String startDate, String endDate, int totalDays,
                        String reason) throws RemoteException;

        /**
         * Get all leave applications for a specific employee
         *
         * @param userId Employee's UID
         * @return Formatted string with leave history
         */
        String getLeavesByUserId(String userId) throws RemoteException;

        /**
         * Get leave balance for a specific employee
         * Also checks and resets if new year has begun
         *
         * @param userId Employee's UID
         * @return Formatted string with leave balance details
         */
        String getLeaveBalance(String userId) throws RemoteException;

        /**
         * Get leave balance data as a Map for programmatic access
         * Returns annual, emergency, and medical leave balances
         *
         * @param userId Employee's UID
         * @return Map with keys: "annual", "emergency", "medical" (values as Integer)
         */
        java.util.Map<String, Integer> getLeaveBalanceData(String userId) throws RemoteException;

        /**
         * Check and reset leave balance if we are in a new year
         * Called when employee logs in or accesses leave features
         *
         * @param userId Employee's UID
         * @return true if reset was performed or no reset needed
         */
        boolean checkAndResetLeaveBalance(String userId) throws RemoteException;

        // ==================== HR LEAVE MANAGEMENT ====================

        /**
         * Get all pending leave requests (HR only)
         * Filters Leave_Request collection for status = "Pending"
         *
         * @return Formatted string with all pending leave requests
         */
        String getAllPendingLeaves() throws RemoteException;

        /**
         * Approve a leave request
         * Checks if employee has sufficient balance before approving
         * Deducts leave days from appropriate leave type upon approval
         *
         * @param leaveId Leave request ID
         * @return Success/error message
         */
        String approveLeave(String leaveId) throws RemoteException;

        /**
         * Reject a leave request
         * No balance deduction occurs when rejecting
         *
         * @param leaveId Leave request ID
         * @return Success/error message
         */
        String rejectLeave(String leaveId) throws RemoteException;
}
