# Lesson 3: Design Principles

---

## General Principles

| Principle | One-Liner |
|-----------|-----------|
| **KISS** | Keep It Simple — avoid unnecessary complexity |
| **DRY** | Don't Repeat Yourself — single source of truth |
| **YAGNI** | You Aren't Gonna Need It — don't build for hypotheticals |
| **Separation of Concerns** | Each class handles one concern |
| **Law of Demeter** | Only talk to your immediate friends |

---

## KISS — Keep It Simple, Stupid

Prefer the simplest solution that works. Avoid over-engineering.

```java
// BAD: over-engineered for a simple need
public class UserValidator {
    private final ValidationStrategy strategy;
    private final ValidatorChain chain;
    private final ValidationContext context;
    // ... unnecessary complexity for a simple email check
}

// GOOD: direct and simple
public class UserValidator {
    public boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
```

---

## DRY — Don't Repeat Yourself

Every piece of knowledge must have a single, authoritative representation.

```java
// BAD: fee calculation duplicated in two places
public class ParkingLot {
    public double calculateCarFee(int hours) {
        return hours * 20.0;
    }
    public double calculateExitFee(int hours) {
        return hours * 20.0;  // duplicated magic number and logic
    }
}

// GOOD: single source of truth
public class ParkingLot {
    private static final double HOURLY_RATE = 20.0;

    public double calculateFee(int hours) {
        return hours * HOURLY_RATE;
    }
}
```

---

## YAGNI — You Aren't Gonna Need It

Don't add features or abstractions for hypothetical future requirements.

```java
// BAD: added "just in case we need multiple databases"
public interface UserRepository {
    User findById(String id);
}
public class MySQLUserRepository implements UserRepository { ... }
public class MongoUserRepository implements UserRepository { ... }  // never needed
public class DynamoUserRepository implements UserRepository { ... } // never needed

// GOOD: only what's required today
public class UserRepository {
    private final Map<String, User> store = new HashMap<>();

    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
```

---

## Separation of Concerns

Each class/module should have exactly one reason to change.

```java
// BAD: Order class handles business logic, persistence, AND email
public class Order {
    public void placeOrder() {
        // validate items
        // calculate price
        // save to database         <-- concerns mixed
        // send confirmation email  <-- concerns mixed
    }
}

// GOOD: each concern in its own class
public class Order {
    public boolean isValid() { ... }
    public double calculateTotal() { ... }
}

public class OrderRepository {
    public void save(Order order) { ... }
}

public class OrderNotificationService {
    public void sendConfirmation(Order order) { ... }
}
```

---

## Law of Demeter

"Only talk to your immediate friends." Avoid long method chains.

```java
// BAD: violates Law of Demeter (too many dots)
public void printCity(Order order) {
    String city = order.getCustomer().getAddress().getCity().getName();
    System.out.println(city);
}

// GOOD: each class exposes what it needs to
public class Order {
    public String getCustomerCity() {
        return customer.getCity();  // Order asks Customer, not the whole chain
    }
}

public void printCity(Order order) {
    System.out.println(order.getCustomerCity());
}
```

---

## SOLID Principles

---

### S — Single Responsibility Principle (SRP)

> A class should have only **one reason to change**.

```java
// BAD: UserManager has 3 responsibilities
public class UserManager {
    public void authenticate(String username, String password) { ... }  // auth
    public void updateProfile(User user) { ... }                        // profile
    public void sendWelcomeEmail(User user) { ... }                     // email
}

// GOOD: one responsibility each
public class AuthService {
    public boolean authenticate(String username, String password) { ... }
}

public class UserProfileService {
    public void updateProfile(User user) { ... }
}

public class EmailService {
    public void sendWelcomeEmail(User user) { ... }
}
```

---

### O — Open/Closed Principle (OCP)

> Classes should be **open for extension, closed for modification**.  
> Add new behavior by adding new classes — don't modify existing ones.

```java
// BAD: must modify PaymentProcessor every time a new payment method is added
public class PaymentProcessor {
    public void process(String type, double amount) {
        if (type.equals("CREDIT"))  { /* credit logic */ }
        else if (type.equals("DEBIT"))  { /* debit logic */ }
        else if (type.equals("CRYPTO")) { /* crypto logic */ } // modification
    }
}

// GOOD: extend via new implementations, never modify existing
public interface PaymentMethod {
    void process(double amount);
}

public class CreditCardPayment implements PaymentMethod {
    @Override
    public void process(double amount) { /* credit logic */ }
}

public class DebitCardPayment implements PaymentMethod {
    @Override
    public void process(double amount) { /* debit logic */ }
}

public class CryptoPayment implements PaymentMethod {    // NEW — no existing class touched
    @Override
    public void process(double amount) { /* crypto logic */ }
}

public class PaymentProcessor {
    public void process(PaymentMethod method, double amount) {
        method.process(amount);  // works with any current or future implementation
    }
}
```

---

### L — Liskov Substitution Principle (LSP)

> Subtypes must be **substitutable for their base types** without breaking correctness.

```java
// BAD: Square breaks Rectangle's contract
public class Rectangle {
    protected int width, height;

    public void setWidth(int w)  { this.width = w; }
    public void setHeight(int h) { this.height = h; }

    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        this.width = w;
        this.height = w;   // breaks expectation: setting width changes height
    }

    @Override
    public void setHeight(int h) {
        this.width = h;
        this.height = h;
    }
}

// Violates LSP:
Rectangle r = new Square();
r.setWidth(4);
r.setHeight(5);
System.out.println(r.area()); // Expected 20, got 25 — BROKEN

// GOOD: use a common abstraction instead of inheritance
public interface Shape {
    int area();
}

public class Rectangle implements Shape { ... }
public class Square implements Shape { ... }
```

---

### I — Interface Segregation Principle (ISP)

> No client should be forced to depend on methods it **doesn't use**.  
> Prefer many small, specific interfaces over one fat interface.

```java
// BAD: one fat interface forces all vehicle types to implement unneeded methods
public interface Vehicle {
    void drive();
    void fly();
    void sail();
}

public class Car implements Vehicle {
    public void drive() { /* OK */ }
    public void fly()   { throw new UnsupportedOperationException(); } // forced!
    public void sail()  { throw new UnsupportedOperationException(); } // forced!
}

// GOOD: segregated interfaces
public interface Drivable { void drive(); }
public interface Flyable  { void fly();   }
public interface Sailable { void sail();  }

public class Car implements Drivable {
    public void drive() { /* only what Car needs */ }
}

public class Airplane implements Drivable, Flyable {
    public void drive() { /* taxi on runway */ }
    public void fly()   { /* flight logic */  }
}
```

---

### D — Dependency Inversion Principle (DIP)

> High-level modules should **not depend on low-level modules**.  
> Both should depend on **abstractions**.

```java
// BAD: Game is tightly coupled to ConsoleRenderer
public class Game {
    private final ConsoleRenderer renderer = new ConsoleRenderer(); // hardcoded

    public void render() {
        renderer.draw(board);
    }
}

// GOOD: Game depends on an abstraction; implementation is injected
public interface Renderer {
    void draw(Board board);
}

public class ConsoleRenderer implements Renderer {
    @Override
    public void draw(Board board) { /* console output */ }
}

public class WebRenderer implements Renderer {
    @Override
    public void draw(Board board) { /* web output */ }
}

public class Game {
    private final Renderer renderer;  // depends on abstraction

    public Game(Renderer renderer) {  // injected — could be Console, Web, Mock
        this.renderer = renderer;
    }

    public void render() {
        renderer.draw(board);
    }
}
```

---

## Principles Quick Reference

| Principle | Trigger Question | Fix |
|-----------|-----------------|-----|
| SRP | "This class changes for multiple reasons" | Split into smaller classes |
| OCP | "Adding a feature requires modifying existing code" | Add interface, create new impl |
| LSP | "Subclass throws UnsupportedOperationException" | Rethink hierarchy |
| ISP | "Class implements methods it doesn't use" | Split interface |
| DIP | "High-level class `new`s a low-level class directly" | Inject the dependency |
| DRY | "This logic is in two places" | Extract to shared method/constant |
| KISS | "This feels over-engineered" | Simplify |
| YAGNI | "We might need this later" | Delete it |
