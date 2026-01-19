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
    public String addEmployee(String email, String password, String firstName, String lastName, String icPassport, String role) throws RemoteException {
        System.out.println("Server: Adding employee - " + email);
        return authService.addEmployee(email, password, firstName, lastName, icPassport, role);
    }
}
