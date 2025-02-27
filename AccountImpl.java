import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountImpl extends UnicastRemoteObject implements Account {
    private Connection conn;

    public AccountImpl() throws RemoteException {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("JDBC Driver loaded successfully"); // Debug
            // Connect to MySQL with your credentials
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/banking_db", "root", "Senior@ceo1");
            System.out.println("Connected to MySQL database successfully"); 
        } catch (ClassNotFoundException e) {
            throw new RemoteException("Failed to load JDBC driver: " + e.getMessage());
        } catch (SQLException e) {
            throw new RemoteException("Failed to connect to MySQL: " + e.getMessage());
        }
    }

    private void verifyPin(String accountNumber, String pin) throws RemoteException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT pin FROM accounts WHERE account_number = ?")) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new RemoteException("Account not found");
            }
            if (!rs.getString("pin").equals(pin)) {
                throw new RemoteException("Incorrect PIN");
            }
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }

    @Override
    public void deposit(String accountNumber, double amount, String pin) throws RemoteException {
        verifyPin(accountNumber, pin);
        if (amount <= 0) throw new RemoteException("Amount must be positive");
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE accounts SET balance = balance + ? WHERE account_number = ?")) {
            stmt.setDouble(1, amount);
            stmt.setString(2, accountNumber);
            int rows = stmt.executeUpdate();
            if (rows == 0) throw new RemoteException("Account not found");
            System.out.println("Deposited KSH" + amount + " into " + accountNumber);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }

    @Override
    public void withdraw(String accountNumber, double amount, String pin) throws RemoteException {
        verifyPin(accountNumber, pin);
        if (amount <= 0) throw new RemoteException("Amount must be positive");
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?")) {
            stmt.setDouble(1, amount);
            stmt.setString(2, accountNumber);
            stmt.setDouble(3, amount);
            int rows = stmt.executeUpdate();
            if (rows == 0) throw new RemoteException("Insufficient funds or account not found");
            System.out.println("Withdrawn KSH" + amount + " from " + accountNumber);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }

    @Override
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount, String pin) throws RemoteException {
        verifyPin(fromAccountNumber, pin);
        if (!balancesContains(toAccountNumber)) throw new RemoteException("Destination account not found");
        if (fromAccountNumber.equals(toAccountNumber)) throw new RemoteException("Cannot transfer to same account");
        if (amount <= 0) throw new RemoteException("Amount must be positive");

        try {
            conn.setAutoCommit(false);
            double fromBalance = getBalance(fromAccountNumber);
            System.out.println("Before transfer: " + fromAccountNumber + " balance = KSH" + fromBalance);
            if (amount > fromBalance) {
                conn.rollback();
                throw new RemoteException("Insufficient funds");
            }

            PreparedStatement withdrawStmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
            withdrawStmt.setDouble(1, amount);
            withdrawStmt.setString(2, fromAccountNumber);
            withdrawStmt.executeUpdate();

            PreparedStatement depositStmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_number = ?");
            depositStmt.setDouble(1, amount);
            depositStmt.setString(2, toAccountNumber);
            depositStmt.executeUpdate();

            conn.commit();
            System.out.println("Transferred KSH" + amount + " from " + fromAccountNumber + " to " + toAccountNumber);
            System.out.println("After transfer: " + fromAccountNumber + " balance = KSH" + getBalance(fromAccountNumber));
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                throw new RemoteException("Rollback failed: " + rollbackEx.getMessage());
            }
            throw new RemoteException("Database error: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RemoteException("Auto-commit reset failed: " + e.getMessage());
            }
        }
    }

    @Override
    public double getBalance(String accountNumber, String pin) throws RemoteException {
        verifyPin(accountNumber, pin);
        return getBalance(accountNumber);
    }

    @Override
    public double getBalance(String accountNumber) throws RemoteException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?")) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) throw new RemoteException("Account not found");
            return rs.getDouble("balance");
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }

    @Override
    public String[] getAllAccountNumbers() throws RemoteException {
        List<String> accountNumbers = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT account_number FROM accounts")) {
            while (rs.next()) {
                accountNumbers.add(rs.getString("account_number"));
            }
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
        return accountNumbers.toArray(new String[0]);
    }

    private boolean balancesContains(String accountNumber) throws RemoteException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM accounts WHERE account_number = ?")) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
}