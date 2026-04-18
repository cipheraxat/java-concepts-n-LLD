package lessons.oop_concepts;

import java.time.LocalDateTime;

/**
 * LESSON 01E — INTERFACES (DEEP DIVE)
 *
 * What this demo covers:
 *  1. Interface as a contract
 *  2. Multiple interface implementation
 *  3. Default methods (Java 8+)
 *  4. Static methods in interfaces
 *  5. Conflict resolution when two interfaces have same default method
 *  6. Marker interface usage
 */
public class InterfaceDemo {

    // 1) Core contract
    interface PaymentGateway {
        PaymentResult pay(String orderId, double amount);

        default boolean supportsRefund() {
            return true;
        }

        static void printAudit(String gatewayName, String orderId, double amount) {
            System.out.printf("[%s] Gateway=%s, Order=%s, Amount=%.2f%n",
                    LocalDateTime.now(), gatewayName, orderId, amount);
        }
    }

    // 2) Additional capability interfaces
    interface UpiCapable {
        void validateVpa(String vpa);

        default String methodName() {
            return "UPI";
        }
    }

    interface CardCapable {
        void maskCard(String cardNumber);

        default String methodName() {
            return "CARD";
        }
    }

    // 3) Marker interface (no methods) for premium providers
    interface PremiumProvider {
    }

    static class PaymentResult {
        private final boolean success;
        private final String message;

        PaymentResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        @Override
        public String toString() {
            return "PaymentResult{success=" + success + ", message='" + message + "'}";
        }
    }

    // 4) Class implementing multiple interfaces
    static class FastPayGateway implements PaymentGateway, UpiCapable, PremiumProvider {

        @Override
        public PaymentResult pay(String orderId, double amount) {
            PaymentGateway.printAudit("FastPay", orderId, amount);
            return new PaymentResult(true, "UPI payment accepted");
        }

        @Override
        public void validateVpa(String vpa) {
            if (vpa == null || !vpa.contains("@")) {
                throw new IllegalArgumentException("Invalid VPA: " + vpa);
            }
            System.out.println("VPA validated: " + vpa);
        }
    }

    // 5) Class implementing interfaces with same default method name
    static class HybridGateway implements UpiCapable, CardCapable, PaymentGateway {

        @Override
        public PaymentResult pay(String orderId, double amount) {
            PaymentGateway.printAudit("Hybrid", orderId, amount);
            return new PaymentResult(true, "Hybrid payment accepted");
        }

        @Override
        public void validateVpa(String vpa) {
            System.out.println("Hybrid VPA check done for: " + vpa);
        }

        @Override
        public void maskCard(String cardNumber) {
            String last4 = cardNumber.substring(cardNumber.length() - 4);
            System.out.println("Card masked: **** **** **** " + last4);
        }

        // Conflict resolution: both UpiCapable and CardCapable define methodName()
        @Override
        public String methodName() {
            return UpiCapable.super.methodName() + "+" + CardCapable.super.methodName();
        }
    }

    private static void printTier(PaymentGateway gateway) {
        if (gateway instanceof PremiumProvider) {
            System.out.println("Provider tier: PREMIUM");
        } else {
            System.out.println("Provider tier: STANDARD");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Interface Contract + Runtime Polymorphism ===");
        PaymentGateway gateway = new FastPayGateway();
        printTier(gateway);
        ((UpiCapable) gateway).validateVpa("john@okbank");
        System.out.println(gateway.pay("ORD-101", 1299.0));
        System.out.println("Supports refund? " + gateway.supportsRefund());

        System.out.println("\n=== Multiple Interfaces + Default Method Conflict ===");
        HybridGateway hybrid = new HybridGateway();
        hybrid.validateVpa("alice@upi");
        hybrid.maskCard("4111111111111111");
        System.out.println("Method name: " + hybrid.methodName());
        System.out.println(hybrid.pay("ORD-102", 499.0));
    }
}
