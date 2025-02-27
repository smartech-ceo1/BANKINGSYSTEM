import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class BankSwingClient extends JFrame {
    private Account account;
    private JTextField accountField, amountField, fromField, toField, pinField;
    private JTextArea resultArea;
    private JComboBox<String> actionCombo;

    public BankSwingClient() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            account = (Account) registry.lookup("AccountService");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to connect to RMI server: " + e.getMessage());
            System.exit(1);
        }

        setTitle("Banking Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel northPanel = new JPanel();
        northPanel.add(new JLabel("Select Action:"));
        String[] actions = {"Check Balance", "Deposit", "Withdraw", "Transfer"};
        actionCombo = new JComboBox<>(actions);
        actionCombo.addActionListener(e -> updateFields());
        northPanel.add(actionCombo);
        add(northPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        centerPanel.add(new JLabel("Account Number:"));
        accountField = new JTextField(10);
        centerPanel.add(accountField);

        centerPanel.add(new JLabel("PIN:"));
        pinField = new JPasswordField(10);
        centerPanel.add(pinField);

        centerPanel.add(new JLabel("Amount (KSH):"));
        amountField = new JTextField(10);
        centerPanel.add(amountField);

        centerPanel.add(new JLabel("From Account:"));
        fromField = new JTextField(10);
        centerPanel.add(fromField);

        centerPanel.add(new JLabel("To Account:"));
        toField = new JTextField(10);
        centerPanel.add(toField);

        add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> performAction());
        southPanel.add(submitButton, BorderLayout.NORTH);

        resultArea = new JTextArea(5, 20);
        resultArea.setEditable(false);
        southPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(southPanel, BorderLayout.SOUTH);

        updateFields();
    }

    private void updateFields() {
        String action = (String) actionCombo.getSelectedItem();
        accountField.setEnabled(!action.equals("Transfer"));
        pinField.setEnabled(true);
        amountField.setEnabled(!action.equals("Check Balance"));
        fromField.setEnabled(action.equals("Transfer"));
        toField.setEnabled(action.equals("Transfer"));
    }

    private void performAction() {
        try {
            String action = (String) actionCombo.getSelectedItem();
            String pin = pinField.getText();
            String result;

            switch (action) {
                case "Check Balance":
                    String accountNum = accountField.getText();
                    double balance = account.getBalance(accountNum, pin);
                    result = "Balance for " + accountNum + ": KSH" + balance;
                    break;

                case "Deposit":
                    accountNum = accountField.getText();
                    double depositAmount = Double.parseDouble(amountField.getText());
                    account.deposit(accountNum, depositAmount, pin);
                    result = "Deposited KSH" + depositAmount + " to " + accountNum + ". New balance: KSH" + account.getBalance(accountNum, pin);
                    break;

                case "Withdraw":
                    accountNum = accountField.getText();
                    double withdrawAmount = Double.parseDouble(amountField.getText());
                    account.withdraw(accountNum, withdrawAmount, pin);
                    result = "Withdrawn KSH" + withdrawAmount + " from " + accountNum + ". New balance: KSH" + account.getBalance(accountNum, pin);
                    break;

                case "Transfer":
                    String fromAccount = fromField.getText();
                    String toAccount = toField.getText();
                    double transferAmount = Double.parseDouble(amountField.getText());
                    account.transfer(fromAccount, toAccount, transferAmount, pin);
                    double fromBalance = account.getBalance(fromAccount);
                    result = "Transferred KSH" + transferAmount + " from " + fromAccount + " to " + toAccount + "\n" +
                             "New balance for " + fromAccount + ": KSH" + fromBalance;
                    break;

                default:
                    result = "Invalid action";
            }
            resultArea.setText(result);
        } catch (RemoteException e) {
            resultArea.setText(e.getMessage());
        } catch (NumberFormatException e) {
            resultArea.setText("Please enter a valid amount in KSH");
        } catch (Exception e) {
            resultArea.setText("An unexpected error occurred: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankSwingClient().setVisible(true));
    }
}