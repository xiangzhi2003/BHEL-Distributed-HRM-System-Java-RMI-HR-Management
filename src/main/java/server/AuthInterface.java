package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthInterface extends Remote {
    // Login
    String login(String email, String password) throws RemoteException;

    String getRole(String uid) throws RemoteException;

    // HR Operations
    String getAllEmployees() throws RemoteException;

    String addEmployee(String email, String password, String firstName, String lastName, String icPassport, String role)
            throws RemoteException;

    String getEmployeeByUid(String uid) throws RemoteException;

    boolean updateEmployee(String uid, String firstName, String lastName, String icPassport, String role)
            throws RemoteException;
}
