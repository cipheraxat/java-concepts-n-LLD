package lessons.oop_concepts;

/**
 * LESSON 01A — ENCAPSULATION
 *
 * Encapsulation = bundling data (fields) + behavior (methods) into one unit,
 * and restricting direct access to internal state.
 *
 * SDE2 Interview: "Explain encapsulation with a real-world example."
 * Answer: A BankAccount exposes deposit()/withdraw() but hides the `balance` field.
 *         The invariant (balance >= 0) is protected inside the class.
 */
public class EncapsulationDemo {

    // ─── Encapsulated class ──────────────────────────────────────────────────────

    static class BankAccount {
        private String owner;
        private double balance;        // private — cannot be set directly from outside
        private static int totalAccounts = 0;

        public BankAccount(String owner, double initialBalance) {
            if (initialBalance < 0) throw new IllegalArgumentException("Balance cannot be negative");
            this.owner = owner;
            this.balance = initialBalance;
            totalAccounts++;
        }

        // Read-only getter — external code cannot directly set balance
        public double getBalance() { return balance; }
        public String getOwner()   { return owner; }

        public void deposit(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");
            balance += amount;
        }

        public void withdraw(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
            if (amount > balance) throw new IllegalStateException("Insufficient funds");
            balance -= amount;
        }

        public static int getTotalAccounts() { return totalAccounts; }

        @Override
        public String toString() {
            return "BankAccount{owner='" + owner + "', balance=" + balance + "}";
        }
    }

    // ─── Benefits of Encapsulation ──────────────────────────────────────────────
    /*
     * 1. Data Hiding    — internal representation can change without affecting clients
     * 2. Validation     — invariants enforced in setters/methods
     * 3. Flexibility    — can add logging, caching, thread-safety inside methods
     * 4. Maintainability— changes are localized
     */

    public static void main(String[] args) {
        BankAccount acc = new BankAccount("Alice", 1000.0);
        acc.deposit(500);
        acc.withdraw(200);
        System.out.println(acc);                         // BankAccount{owner='Alice', balance=1300.0}
        System.out.println("Total accounts: " + BankAccount.getTotalAccounts());

        // acc.balance = -9999;  // COMPILE ERROR — encapsulation working!

        try {
            acc.withdraw(5000);
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage()); // Insufficient funds
        }
    }
}
