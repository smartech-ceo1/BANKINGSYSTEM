import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BankServer {
    public static void main(String[] args) {
        try {
            Account account = new AccountImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("AccountService", account);
            System.out.println("Bank Server is running...");
            System.out.println("Ready to handle transactions for 10 accounts.");
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace(); // Added to show full stack trace
        }
    }
}