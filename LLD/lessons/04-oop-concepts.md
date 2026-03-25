# Lesson 4: OOP Concepts

---

## The Four Pillars of OOP

| Pillar | One-Liner |
|--------|-----------|
| **Encapsulation** | Bundle data + behavior; hide internal state |
| **Abstraction** | Expose what's needed; hide complexity |
| **Polymorphism** | Same interface, different behavior |
| **Inheritance** | Reuse and extend existing types |

---

## 1. Encapsulation

Bundle data and the methods that operate on it within a single class. Hide internal state — only expose what is necessary.

```java
// BAD: fields are public — anyone can break invariants
public class BankAccount {
    public double balance;      // direct access — no protection
    public String accountId;
}

// Usage: anyone can do account.balance = -999; (breaks invariant)

// GOOD: private fields, controlled access via methods
public class BankAccount {
    private final String accountId;
    private double balance;

    public BankAccount(String accountId, double initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative");
        this.accountId = accountId;
        this.balance = initialBalance;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        balance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        return true;
    }

    public double getBalance() { return balance; }  // read-only view
    public String getAccountId() { return accountId; }

    // No setter for balance — only deposit/withdraw control it
}
```

**Key Rules:**
- Fields are `private` by default
- Provide `getters` only when callers need to read state
- Provide `setters` only when callers need to write state, with validation
- Never expose mutable internal collections directly — return copies or unmodifiable views

```java
// BAD: exposes internal list — caller can modify it
public List<String> getItems() { return items; }

// GOOD: return unmodifiable view
public List<String> getItems() { return Collections.unmodifiableList(items); }
```

---

## 2. Abstraction

Expose only what's necessary. Hide complexity behind simple interfaces.

```java
// Abstraction via interface — callers don't care HOW, only WHAT
public interface PaymentGateway {
    boolean charge(String customerId, double amount);
    boolean refund(String transactionId);
}

// Hidden complexity inside the implementation
public class StripeGateway implements PaymentGateway {
    private final StripeClient client;

    @Override
    public boolean charge(String customerId, double amount) {
        // Complex retry logic, currency conversion, fraud detection
        // All hidden from the caller
        StripeCharge charge = client.charges().create(
            new ChargeParams().amount((long)(amount * 100)).currency("usd")
        );
        return "succeeded".equals(charge.getStatus());
    }

    @Override
    public boolean refund(String transactionId) {
        // Complex refund logic, hidden
        return client.refunds().create(new RefundParams().charge(transactionId))
                     .getStatus().equals("succeeded");
    }
}

// Caller only sees the abstraction
public class OrderService {
    private final PaymentGateway gateway;  // doesn't know it's Stripe

    public OrderService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public boolean checkout(Order order) {
        return gateway.charge(order.getCustomerId(), order.getTotal());
    }
}
```

**Abstract Classes vs Interfaces:**

| Aspect | `interface` | `abstract class` |
|--------|-------------|-----------------|
| State | No instance fields (only `static final`) | Can have instance fields |
| Constructor | No | Yes |
| Multiple inheritance | Yes (implement multiple) | No (extends one) |
| When to use | Pure contract / capability | Shared state + partial implementation |

```java
// Use abstract class when subclasses share common state/behavior
public abstract class Piece {
    protected final Color color;
    protected Position position;

    public Piece(Color color, Position position) {
        this.color = color;
        this.position = position;
    }

    // Shared behavior
    public Color getColor() { return color; }
    public Position getPosition() { return position; }
    public void moveTo(Position pos) { this.position = pos; }

    // Subclass-specific behavior — must override
    public abstract List<Position> getValidMoves(Board board);
}

public class Rook extends Piece {
    public Rook(Color color, Position position) { super(color, position); }

    @Override
    public List<Position> getValidMoves(Board board) {
        // Rook-specific: horizontal and vertical moves
        List<Position> moves = new ArrayList<>();
        // ... rook logic
        return moves;
    }
}
```

---

## 3. Polymorphism

One interface (method name), many implementations. Enables writing code that works with any subtype.

### Runtime Polymorphism (Method Overriding)

```java
public interface Shape {
    double area();
    double perimeter();
}

public class Circle implements Shape {
    private final double radius;

    public Circle(double radius) { this.radius = radius; }

    @Override
    public double area() { return Math.PI * radius * radius; }

    @Override
    public double perimeter() { return 2 * Math.PI * radius; }
}

public class Rectangle implements Shape {
    private final double width, height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() { return width * height; }

    @Override
    public double perimeter() { return 2 * (width + height); }
}

public class Triangle implements Shape {
    private final double a, b, c;  // sides

    public Triangle(double a, double b, double c) {
        this.a = a; this.b = b; this.c = c;
    }

    @Override
    public double area() {
        double s = (a + b + c) / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }

    @Override
    public double perimeter() { return a + b + c; }
}

// Polymorphic usage — code doesn't care which Shape it is
public class ShapeCalculator {
    public static double totalArea(List<Shape> shapes) {
        return shapes.stream()
                     .mapToDouble(Shape::area)
                     .sum();
    }

    public static Shape largest(List<Shape> shapes) {
        return shapes.stream()
                     .max(Comparator.comparingDouble(Shape::area))
                     .orElseThrow();
    }
}
```

### Compile-Time Polymorphism (Method Overloading)

```java
public class MessageSender {
    public void send(String message) {
        System.out.println("Text: " + message);
    }

    public void send(String message, String recipient) {
        System.out.println("To " + recipient + ": " + message);
    }

    public void send(List<String> messages) {
        messages.forEach(this::send);
    }
}
```

---

## 4. Inheritance vs Composition

### Inheritance (is-a)

```java
public abstract class Animal {
    protected String name;

    public Animal(String name) { this.name = name; }

    public abstract String makeSound();

    public String describe() {
        return name + " says: " + makeSound();
    }
}

public class Dog extends Animal {
    public Dog(String name) { super(name); }

    @Override
    public String makeSound() { return "Woof"; }
}

public class Cat extends Animal {
    public Cat(String name) { super(name); }

    @Override
    public String makeSound() { return "Meow"; }
}
```

### Composition (has-a) — Preferred

```java
// BAD: using inheritance to "reuse" behavior
public class Stack<T> extends ArrayList<T> {
    // Inherits all ArrayList methods: get, set, add at index, etc.
    // None of those belong on a Stack!
}

// GOOD: composition — wrap the list, expose only what Stack needs
public class Stack<T> {
    private final List<T> elements = new ArrayList<>();

    public void push(T item) { elements.add(item); }

    public Optional<T> pop() {
        if (elements.isEmpty()) return Optional.empty();
        return Optional.of(elements.remove(elements.size() - 1));
    }

    public Optional<T> peek() {
        if (elements.isEmpty()) return Optional.empty();
        return Optional.of(elements.get(elements.size() - 1));
    }

    public boolean isEmpty() { return elements.isEmpty(); }
    public int size() { return elements.size(); }
}
```

### Composition for Behavior Reuse

```java
// Separate capabilities as composable interfaces and classes
public interface Logger {
    void log(String message);
}

public class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
}

public interface EmailNotifier {
    void notify(String to, String message);
}

public class SmtpEmailNotifier implements EmailNotifier {
    @Override
    public void notify(String to, String message) {
        // SMTP sending logic
    }
}

// Compose services without deep inheritance hierarchies
public class OrderService {
    private final Logger logger;
    private final EmailNotifier emailNotifier;

    public OrderService(Logger logger, EmailNotifier emailNotifier) {
        this.logger = logger;
        this.emailNotifier = emailNotifier;
    }

    public void placeOrder(Order order) {
        // ... logic
        logger.log("Order placed: " + order.getId());
        emailNotifier.notify(order.getCustomerEmail(), "Your order has been placed!");
    }
}
```

---

## When to Use Each

| Situation | Use |
|-----------|-----|
| True "is-a" relationship (Dog is-a Animal) | Inheritance |
| Reusing behavior without type relationship | Composition |
| Multiple behaviors to combine | Composition (implement multiple interfaces) |
| Need to swap behavior at runtime | Composition + Strategy pattern |
| Rigid, deep hierarchy needed | Reconsider — prefer composition |

> **Rule of Thumb:** Default to **composition**. Reach for inheritance only when you have a genuine `is-a` relationship and want to leverage polymorphism.

---

## Returning Defensive Copies

```java
// Protect mutable objects in encapsulation
public class Team {
    private final List<String> members = new ArrayList<>();

    public void addMember(String name) { members.add(name); }

    // BAD: caller can modify internal list
    public List<String> getMembers() { return members; }

    // GOOD: return a copy
    public List<String> getMembers() { return new ArrayList<>(members); }

    // BETTER: return unmodifiable view (no copy needed)
    public List<String> getMembers() { return Collections.unmodifiableList(members); }
}
```
