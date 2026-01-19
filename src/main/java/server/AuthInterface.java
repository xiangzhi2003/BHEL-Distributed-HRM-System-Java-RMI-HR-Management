package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthInterface extends Remote {
    // Login
    String login(String email, String password) throws RemoteException;

    String getRole(String uid) throws RemoteException;
}
