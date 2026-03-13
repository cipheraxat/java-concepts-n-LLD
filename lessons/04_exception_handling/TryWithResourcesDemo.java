package lessons.exception_handling;

/**
 * LESSON 04B — try-with-resources + Custom Exceptions
 *
 * try-with-resources (Java 7):
 *  - Auto-closes resources that implement AutoCloseable (or Closeable)
 *  - Resources closed in REVERSE order of opening
 *  - If both resource close AND catch block throw, close exception is SUPPRESSED
 *    (accessible via e.getSuppressed())
 *
 * SDE2 Interview:
 *  - "How is try-with-resources better than try-finally for resources?"
 *    => Correct close order, handles suppressed exceptions, cleaner code
 *  - "What if close() throws in try-with-resources?"
 *    => If try body also throws, close exception is suppressed (not lost, but secondary)
 *       If try body doesn't throw, close exception propagates normally
 */
public class TryWithResourcesDemo {

    // ─── Simulated resources ────────────────────────────────────────────────────
    static class DatabaseConnection implements AutoCloseable {
        String name;
        DatabaseConnection(String name) {
            this.name = name;
            System.out.println("[OPEN] " + name);
        }

        void query(String sql) {
            System.out.println("[QUERY] " + name + ": " + sql);
        }

        @Override
        public void close() {
            System.out.println("[CLOSE] " + name);
            // In real code: connection.close()
        }
    }

    static class FailingResource implements AutoCloseable {
        FailingResource() { System.out.println("[OPEN] FailingResource"); }

        void use() { throw new RuntimeException("Exception in use()"); }

        @Override
        public void close() {
            System.out.println("[CLOSE] FailingResource");
            throw new RuntimeException("Exception in close()");  // close also throws!
        }
    }

    // ─── Custom Exceptions ──────────────────────────────────────────────────────
    // Best practice: extend RuntimeException for application domain exceptions
    static class InsufficientFundsException extends RuntimeException {
        private final double amount;
        private final double balance;

        InsufficientFundsException(double amount, double balance) {
            super(String.format("Cannot withdraw %.2f, current balance: %.2f", amount, balance));
            this.amount = amount;
            this.balance = balance;
        }

        // Exception chaining constructor — always provide cause
        InsufficientFundsException(double amount, double balance, Throwable cause) {
            super(String.format("Cannot withdraw %.2f, current balance: %.2f", amount, balance), cause);
            this.amount = amount;
            this.balance = balance;
        }

        double getAmount()  { return amount; }
        double getBalance() { return balance; }
    }

    static class UserNotFoundException extends RuntimeException {
        private final long userId;

        UserNotFoundException(long userId) {
            super("User not found: " + userId);
            this.userId = userId;
        }

        long getUserId() { return userId; }
    }

    // ─── Service using custom exceptions ─────────────────────────────────────────
    static class AccountService {
        double balance = 100.0;

        void transferTo(long targetUserId, double amount) {
            if (targetUserId == 999L) throw new UserNotFoundException(targetUserId);
            if (amount > balance) throw new InsufficientFundsException(amount, balance);
            balance -= amount;
            System.out.println("Transferred " + amount + ", new balance: " + balance);
        }
    }

    public static void main(String[] args) {

        System.out.println("=== Basic try-with-resources ===");
        try (DatabaseConnection conn = new DatabaseConnection("primary-db")) {
            conn.query("SELECT * FROM users");
            // conn is automatically closed after this block
        }
        System.out.println();

        // ── Multiple resources — closed in REVERSE order ──────────────────────
        System.out.println("=== Multiple Resources (reverse close order) ===");
        try (DatabaseConnection primary   = new DatabaseConnection("primary-db");
             DatabaseConnection secondary = new DatabaseConnection("secondary-db")) {
            primary.query("BEGIN TRANSACTION");
            secondary.query("SELECT config");
            primary.query("COMMIT");
            // secondary closes FIRST, then primary
        }
        System.out.println();

        // ── Suppressed exceptions ─────────────────────────────────────────────
        System.out.println("=== Suppressed Exceptions ===");
        try (FailingResource r = new FailingResource()) {
            r.use();  // throws RuntimeException("Exception in use()")
            // close() also throws, but since use() already threw, close exception is SUPPRESSED
        } catch (RuntimeException e) {
            System.out.println("Caught: " + e.getMessage());  // "Exception in use()"
            Throwable[] suppressed = e.getSuppressed();
            for (Throwable s : suppressed) {
                System.out.println("Suppressed: " + s.getMessage());  // "Exception in close()"
            }
        }
        System.out.println();

        // ── Custom Exceptions ─────────────────────────────────────────────────
        System.out.println("=== Custom Exceptions ===");
        AccountService service = new AccountService();

        try {
            service.transferTo(1L, 50.0);    // OK
            service.transferTo(1L, 200.0);   // throws InsufficientFundsException
        } catch (InsufficientFundsException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.printf("Tried: %.2f, Had: %.2f%n", e.getAmount(), e.getBalance());
        }

        try {
            service.transferTo(999L, 10.0);  // throws UserNotFoundException
        } catch (UserNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("userId: " + e.getUserId());
        }

        System.out.println("\n=== Exception Best Practices ===");
        /*
         * DO:
         *  ✓ Use try-with-resources for any AutoCloseable
         *  ✓ Include meaningful message and cause in custom exceptions
         *  ✓ Use specific exception types (not generic Exception)
         *  ✓ Add domain-specific fields (amount, userId) for error handling
         *  ✓ Log at the boundary (controller/service layer), not deep inside
         *  ✓ Wrap checked exceptions at layer boundaries to preserve abstraction
         *
         * DON'T:
         *  ✗ catch (Exception e) {} — swallows exceptions silently
         *  ✗ catch (Throwable e) — catches Errors too (OOM, StackOverflow)
         *  ✗ Use exceptions for control flow (performance, readability)
         *  ✗ throw new Exception("message") without cause when wrapping
         *  ✗ Declare checked exceptions broadly in API (forces callers to handle)
         */
    }
}
