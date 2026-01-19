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
}
