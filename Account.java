import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    void deposit(String accountNumber, double amount, String pin) throws RemoteException;
    void withdraw(String accountNumber, double amount, String pin) throws RemoteException;
    void transfer(String fromAccountNumber, String toAccountNumber, double amount, String pin) throws RemoteException;
    double getBalance(String accountNumber, String pin) throws RemoteException;
    double getBalance(String accountNumber) throws RemoteException;
    String[] getAllAccountNumbers() throws RemoteException;
}