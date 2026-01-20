package server;

import database.AuthService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthInterface {

    private AuthService authService;

    public AuthServiceImpl() throws RemoteException {
        super();
        authService = new AuthService();
    }

    @Override
    public String login(String email, String password) throws RemoteException {
        System.out.println("Server: Login request for " + email);
        return authService.login(email, password);
    }

    @Override
    public String getRole(String uid) throws RemoteException {
        System.out.println("Server: Getting role for UID " + uid);
        return authService.getRole(uid);
    }

    @Override
    public String getAllEmployees() throws RemoteException {
        System.out.println("Server: Getting all employees");
        return authService.getAllEmployees();
    }

    @Override
    public String addEmployee(String email, String password, String firstName, String lastName, String icPassport, String role) throws RemoteException {
        System.out.println("Server: Adding employee - " + email);
        return authService.addEmployee(email, password, firstName, lastName, icPassport, role);
    }

    @Override
    public String getEmployeeByUid(String uid) throws RemoteException {
        System.out.println("Server: Getting employee - " + uid);
        return authService.getEmployeeByUid(uid);
    }

    @Override
    public boolean updateEmployee(String uid, String firstName, String lastName, String icPassport, String role) throws RemoteException {
        System.out.println("Server: Updating employee - " + uid);
        return authService.updateEmployee(uid, firstName, lastName, icPassport, role);
    }

    @Override
    public boolean deleteEmployee(String uid) throws RemoteException {
        System.out.println("Server: Deleting employee - " + uid);
        return authService.deleteEmployee(uid);
    }

    // Payroll Operations
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
    public boolean updatePayroll(String payrollId, double salary, String monthEntry, String yearEntry) throws RemoteException {
        System.out.println("Server: Updating payroll - " + payrollId);
        return authService.updatePayroll(payrollId, salary, monthEntry, yearEntry);
    }

    @Override
    public boolean deletePayroll(String payrollId) throws RemoteException {
        System.out.println("Server: Deleting payroll - " + payrollId);
        return authService.deletePayroll(payrollId);
    }
}
