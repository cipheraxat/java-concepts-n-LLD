package lessons.solid_principles;

import java.util.*;
import java.util.function.*;

/**
 * LESSON 10 — SOLID Principles (SDE2 Interview)
 *
 * SOLID = 5 principles for designing maintainable, scalable OO software
 *
 *  S — Single Responsibility Principle (SRP)
 *  O — Open/Closed Principle (OCP)
 *  L — Liskov Substitution Principle (LSP)
 *  I — Interface Segregation Principle (ISP)
 *  D — Dependency Inversion Principle (DIP)
 *
 * SDE2 Interview Questions:
 *  - "Explain SOLID with real code examples"
 *  - "How does DIP relate to Dependency Injection?"
 *  - "Give an example of LSP violation"
 *  - "How does ISP prevent fat interfaces?"
 */
public class SOLIDDemo {

    // ═══════════════════════════════════════════════════════════════════════════
    // S — SINGLE RESPONSIBILITY PRINCIPLE
    // A class should have ONE reason to change (= one job)
    // ═══════════════════════════════════════════════════════════════════════════

    // ✗ BAD: Order class handles persistence, formatting, AND email — 3 reasons to change
    static class OrderBad {
        String id; double amount;
        void save()         { /* SQL logic */ }
        String toJson()     { return "{\"id\":\"" + id + "\"}"; }
        void sendEmail()    { /* SMTP logic */ }
    }

    // ✓ GOOD: Each class has one responsibility
    record Order(String id, double amount) {}

    static class OrderRepository {
        void save(Order order) { System.out.println("Saving: " + order); }
    }

    static class OrderSerializer {
        String toJson(Order o) { return "{\"id\":\"" + o.id() + "\",\"amount\":" + o.amount() + "}"; }
    }

    static class OrderEmailService {
        void sendConfirmation(Order o) { System.out.println("Email for order " + o.id()); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // O — OPEN/CLOSED PRINCIPLE
    // Classes should be OPEN for extension, CLOSED for modification
    // Add behavior through new classes/methods, not by editing existing ones
    // ═══════════════════════════════════════════════════════════════════════════

    // ✗ BAD: To add a new discount type, you must modify this class
    static class DiscountCalculatorBad {
        double calculate(String type, double price) {
            return switch (type) {
                case "STUDENT"  -> price * 0.8;
                case "EMPLOYEE" -> price * 0.7;
                // Adding "SENIOR" requires modifying this class!
                default -> price;
            };
        }
    }

    // ✓ GOOD: New discount types extend without modifying existing code
    interface DiscountStrategy {
        double apply(double price);
        boolean isEligible(String customerType);
    }

    static class StudentDiscount implements DiscountStrategy {
        public double apply(double price) { return price * 0.8; }
        public boolean isEligible(String t) { return "STUDENT".equals(t); }
    }

    static class EmployeeDiscount implements DiscountStrategy {
        public double apply(double price) { return price * 0.7; }
        public boolean isEligible(String t) { return "EMPLOYEE".equals(t); }
    }

    static class SeniorDiscount implements DiscountStrategy {  // Added without touching other classes!
        public double apply(double price) { return price * 0.85; }
        public boolean isEligible(String t) { return "SENIOR".equals(t); }
    }

    static class DiscountCalculator {
        private final List<DiscountStrategy> strategies;
        DiscountCalculator(List<DiscountStrategy> strategies) { this.strategies = strategies; }

        double calculate(String customerType, double price) {
            return strategies.stream()
                .filter(s -> s.isEligible(customerType))
                .findFirst()
                .map(s -> s.apply(price))
                .orElse(price);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // L — LISKOV SUBSTITUTION PRINCIPLE
    // Subtypes must be substitutable for their base types WITHOUT breaking behavior
    // If S extends T, T reference pointing to S object should work correctly
    // ═══════════════════════════════════════════════════════════════════════════

    // ✗ CLASSIC VIOLATION: Square extends Rectangle
    static class Rectangle {
        protected int width, height;
        void setWidth(int w)  { width = w; }
        void setHeight(int h) { height = h; }
        int area() { return width * height; }
    }

    static class SquareBad extends Rectangle {
        @Override void setWidth(int w)  { width = height = w; }  // width also sets height!
        @Override void setHeight(int h) { width = height = h; }  // VIOLATION
    }
    // Code that works for Rectangle breaks with SquareBad:
    //   Rectangle r = new SquareBad();
    //   r.setWidth(5); r.setHeight(3);
    //   assert r.area() == 15;  // FAILS! SquareBad gives 9

    // ✓ GOOD: Use separate hierarchy or interface
    interface Shape {
        int area();
    }

    record RectangleLSP(int width, int height) implements Shape {
        public int area() { return width * height; }
    }

    record SquareLSP(int side) implements Shape {
        public int area() { return side * side; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // I — INTERFACE SEGREGATION PRINCIPLE
    // Clients should not be forced to depend on methods they don't use
    // Fat interface → split into specific interfaces
    // ═══════════════════════════════════════════════════════════════════════════

    // ✗ BAD: One fat interface forces all implementors to implement everything
    interface WorkerBad {
        void work();
        void eat();
        void sleep();
        void manageTeam();   // Not all workers manage a team!
    }

    // ✓ GOOD: Segregated interfaces — implement only what you need
    interface Workable  { void work(); }
    interface Eatable   { void eat(); }
    interface Sleepable { void sleep(); }
    interface Manageable{ void manageTeam(); }

    // Employee implements only relevant interfaces
    static class Employee implements Workable, Eatable, Sleepable {
        public void work()  { System.out.println("Employee working"); }
        public void eat()   { System.out.println("Employee eating"); }
        public void sleep() { System.out.println("Employee sleeping"); }
    }

    // Manager gets management too
    static class Manager extends Employee implements Manageable {
        public void manageTeam() { System.out.println("Manager managing"); }
    }

    // Robot doesn't need eat/sleep
    static class Robot implements Workable {
        public void work() { System.out.println("Robot working 24/7"); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // D — DEPENDENCY INVERSION PRINCIPLE
    // High-level modules should NOT depend on low-level modules.
    // Both should depend on ABSTRACTIONS (interfaces).
    // Abstractions should not depend on details; details depend on abstractions.
    // === Basis for Dependency Injection (Spring DI, etc.) ===
    // ═══════════════════════════════════════════════════════════════════════════

    // ✗ BAD: High-level OrderService depends directly on concrete MySQLRepository
    static class MySQLOrderRepo {
        void save(String order) { System.out.println("MySQL: saving " + order); }
    }

    static class OrderServiceBad {
        private final MySQLOrderRepo repo = new MySQLOrderRepo(); // HARD-CODED dependency!
        // Can't swap to MongoDB, can't mock in tests
        void placeOrder(String o) { repo.save(o); }
    }

    // ✓ GOOD: Both depend on abstraction
    interface OrderRepo {
        void save(String order);
    }

    static class MySQLRepo implements OrderRepo {
        public void save(String o) { System.out.println("MySQL: " + o); }
    }

    static class MongoRepo implements OrderRepo {
        public void save(String o) { System.out.println("MongoDB: " + o); }
    }

    static class InMemoryRepo implements OrderRepo {  // perfect for tests
        final List<String> saved = new ArrayList<>();
        public void save(String o) { saved.add(o); System.out.println("InMemory: " + o); }
    }

    static class OrderService {
        private final OrderRepo repo;  // depends on abstraction!
        OrderService(OrderRepo repo) { this.repo = repo; }  // Dependency Injection (constructor)
        void placeOrder(String order) { repo.save(order); }
    }

    public static void main(String[] args) {
        System.out.println("=== SRP ===");
        Order order = new Order("ORD-001", 99.99);
        new OrderRepository().save(order);
        System.out.println(new OrderSerializer().toJson(order));
        new OrderEmailService().sendConfirmation(order);

        System.out.println("\n=== OCP ===");
        DiscountCalculator calc = new DiscountCalculator(List.of(
            new StudentDiscount(), new EmployeeDiscount(), new SeniorDiscount()
        ));
        System.out.println("Student  $100: $" + calc.calculate("STUDENT", 100));
        System.out.println("Employee $100: $" + calc.calculate("EMPLOYEE", 100));
        System.out.println("Senior   $100: $" + calc.calculate("SENIOR", 100));
        System.out.println("Regular  $100: $" + calc.calculate("REGULAR", 100));

        System.out.println("\n=== LSP ===");
        List<Shape> shapes = List.of(new RectangleLSP(4, 5), new SquareLSP(4));
        shapes.forEach(s -> System.out.println(s.getClass().getSimpleName() + " area: " + s.area()));

        System.out.println("\n=== ISP ===");
        Robot robot = new Robot();
        robot.work();
        // robot.eat(); // COMPILE ERROR — Robot doesn't implement Eatable (good!)

        Manager mgr = new Manager();
        mgr.work(); mgr.manageTeam();

        System.out.println("\n=== DIP (Dependency Inversion / Injection) ===");
        OrderService mysqlService  = new OrderService(new MySQLRepo());
        OrderService mongoService  = new OrderService(new MongoRepo());
        InMemoryRepo testRepo      = new InMemoryRepo();
        OrderService testService   = new OrderService(testRepo);  // test-friendly!

        mysqlService.placeOrder("ORD-001");
        mongoService.placeOrder("ORD-002");
        testService.placeOrder("ORD-003");
        System.out.println("InMemory saved: " + testRepo.saved);
    }
}
