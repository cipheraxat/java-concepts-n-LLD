# Lesson 5: Design Patterns

Design patterns are reusable solutions to commonly occurring problems in software design.

---

## Pattern Categories

| Category | Patterns |
|----------|---------|
| **Creational** | Factory Method, Builder, Singleton |
| **Structural** | Decorator, Facade |
| **Behavioral** | Strategy, Observer, State Machine |

---

## Creational Patterns

---

### Factory Method

**Intent:** Create objects without specifying the exact class; delegate creation to a factory.

**When to use:** You need to create objects of different types based on a condition.

```java
// Product hierarchy
public interface Notification {
    void send(String message, String recipient);
}

public class EmailNotification implements Notification {
    @Override
    public void send(String message, String recipient) {
        System.out.println("Email to " + recipient + ": " + message);
    }
}

public class SmsNotification implements Notification {
    @Override
    public void send(String message, String recipient) {
        System.out.println("SMS to " + recipient + ": " + message);
    }
}

public class PushNotification implements Notification {
    @Override
    public void send(String message, String recipient) {
        System.out.println("Push to " + recipient + ": " + message);
    }
}

// Factory
public class NotificationFactory {
    public static Notification create(String type) {
        return switch (type.toUpperCase()) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SmsNotification();
            case "PUSH"  -> new PushNotification();
            default      -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}

// Usage
Notification n = NotificationFactory.create("EMAIL");
n.send("Your order shipped!", "user@example.com");
```

**Used in LLD:** Vehicle types in Parking Lot, piece types in Chess, locker size assignment.

---

### Builder

**Intent:** Construct complex objects step by step. Separate construction from representation.

**When to use:** Object has many optional parameters or complex construction logic.

```java
public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maxConnections;
    private final int connectionTimeoutMs;
    private final boolean sslEnabled;

    private DatabaseConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.username = builder.username;
        this.password = builder.password;
        this.maxConnections = builder.maxConnections;
        this.connectionTimeoutMs = builder.connectionTimeoutMs;
        this.sslEnabled = builder.sslEnabled;
    }

    // Getters...

    public static class Builder {
        // Required fields
        private final String host;
        private final String database;

        // Optional fields with defaults
        private int port = 5432;
        private String username = "root";
        private String password = "";
        private int maxConnections = 10;
        private int connectionTimeoutMs = 3000;
        private boolean sslEnabled = false;

        public Builder(String host, String database) {
            this.host = host;
            this.database = database;
        }

        public Builder port(int port)                   { this.port = port; return this; }
        public Builder username(String u)               { this.username = u; return this; }
        public Builder password(String p)               { this.password = p; return this; }
        public Builder maxConnections(int n)            { this.maxConnections = n; return this; }
        public Builder connectionTimeoutMs(int t)       { this.connectionTimeoutMs = t; return this; }
        public Builder sslEnabled(boolean ssl)          { this.sslEnabled = ssl; return this; }

        public DatabaseConfig build() {
            if (host == null || host.isBlank()) throw new IllegalStateException("Host cannot be blank");
            return new DatabaseConfig(this);
        }
    }
}

// Usage — only set what you need
DatabaseConfig config = new DatabaseConfig.Builder("db.example.com", "myapp")
    .port(3306)
    .username("admin")
    .password("s3cr3t")
    .sslEnabled(true)
    .build();
```

---

### Singleton

**Intent:** Ensure a class has only one instance globally.

**When to use:** Exactly one instance makes sense (config manager, logger, connection pool).

```java
// Thread-safe Singleton with double-checked locking
public class ConfigManager {
    private static volatile ConfigManager instance;
    private final Map<String, String> config = new HashMap<>();

    private ConfigManager() {
        // Load config from file/env
        config.put("maxRetries", "3");
        config.put("timeoutMs", "5000");
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {           // double-check
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    public String get(String key) { return config.get(key); }
    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }
}

// Preferred: Bill Pugh (lazy, thread-safe, no locking overhead)
public class Logger {
    private Logger() {}

    private static class Holder {
        private static final Logger INSTANCE = new Logger();
    }

    public static Logger getInstance() {
        return Holder.INSTANCE;
    }

    public void log(String message) {
        System.out.println("[" + java.time.LocalDateTime.now() + "] " + message);
    }
}
```

> **Warning:** Singletons make unit testing harder and introduce hidden global state. Prefer dependency injection when possible.

---

## Structural Patterns

---

### Decorator

**Intent:** Add behavior to objects dynamically by wrapping them. Concrete classes and decorators both implement the same interface.

**When to use:** Add optional features to objects without modifying their class or creating an explosion of subclasses.

```java
public interface Coffee {
    double cost();
    String description();
}

// Base implementation
public class SimpleCoffee implements Coffee {
    @Override public double cost()        { return 2.0; }
    @Override public String description() { return "Coffee"; }
}

// Base Decorator
public abstract class CoffeeDecorator implements Coffee {
    protected final Coffee wrapped;

    public CoffeeDecorator(Coffee coffee) { this.wrapped = coffee; }
}

// Concrete Decorators
public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) { super(coffee); }

    @Override public double cost()        { return wrapped.cost() + 0.5; }
    @Override public String description() { return wrapped.description() + ", Milk"; }
}

public class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) { super(coffee); }

    @Override public double cost()        { return wrapped.cost() + 0.25; }
    @Override public String description() { return wrapped.description() + ", Sugar"; }
}

public class VanillaDecorator extends CoffeeDecorator {
    public VanillaDecorator(Coffee coffee) { super(coffee); }

    @Override public double cost()        { return wrapped.cost() + 0.75; }
    @Override public String description() { return wrapped.description() + ", Vanilla"; }
}

// Usage
Coffee coffee = new VanillaDecorator(new SugarDecorator(new MilkDecorator(new SimpleCoffee())));
System.out.println(coffee.description()); // Coffee, Milk, Sugar, Vanilla
System.out.println(coffee.cost());        // 3.5
```

---

### Facade

**Intent:** Provide a simplified interface to a complex subsystem.

**When to use:** A subsystem has many classes but you want to expose a clean, simple API.

```java
// Complex subsystem classes
public class InventoryService {
    public boolean checkAndReserve(String productId, int qty) {
        System.out.println("Checking and reserving inventory: " + productId);
        return true;
    }
    public void release(String productId, int qty) {
        System.out.println("Releasing inventory: " + productId);
    }
}

public class PaymentService {
    public boolean charge(String customerId, double amount) {
        System.out.println("Charging customer " + customerId + ": $" + amount);
        return true;
    }
    public void refund(String customerId, double amount) {
        System.out.println("Refunding customer " + customerId + ": $" + amount);
    }
}

public class ShippingService {
    public String scheduleDelivery(String orderId, String address) {
        System.out.println("Scheduling delivery for order " + orderId);
        return "TRK-" + orderId;
    }
}

// Facade — hides all the complexity
public class OrderFacade {
    private final InventoryService inventoryService = new InventoryService();
    private final PaymentService paymentService = new PaymentService();
    private final ShippingService shippingService = new ShippingService();

    public String placeOrder(String customerId, String productId, int qty, double price, String address) {
        if (!inventoryService.checkAndReserve(productId, qty)) {
            throw new RuntimeException("Product out of stock");
        }
        if (!paymentService.charge(customerId, price * qty)) {
            inventoryService.release(productId, qty);
            throw new RuntimeException("Payment failed");
        }
        String trackingNumber = shippingService.scheduleDelivery(customerId + "-order", address);
        return trackingNumber;
    }
}

// Caller only uses the simple facade
OrderFacade facade = new OrderFacade();
String tracking = facade.placeOrder("cust-1", "prod-42", 2, 29.99, "123 Main St");
```

---

## Behavioral Patterns

---

### Strategy

**Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable at runtime.

**When to use:** You need to switch between different algorithms or approaches at runtime.

```java
public interface SortingStrategy {
    void sort(int[] array);
}

public class BubbleSortStrategy implements SortingStrategy {
    @Override
    public void sort(int[] array) { /* bubble sort implementation */ }
}

public class QuickSortStrategy implements SortingStrategy {
    @Override
    public void sort(int[] array) { /* quicksort implementation */ }
}

public class MergeSortStrategy implements SortingStrategy {
    @Override
    public void sort(int[] array) { /* merge sort implementation */ }
}

public class Sorter {
    private SortingStrategy strategy;

    public Sorter(SortingStrategy strategy) {
        this.strategy = strategy;
    }

    // Strategy can be swapped at runtime
    public void setStrategy(SortingStrategy strategy) {
        this.strategy = strategy;
    }

    public void sort(int[] array) {
        strategy.sort(array);
    }
}

// Pricing example for LLD
public interface PricingStrategy {
    double calculatePrice(double basePrice);
}

public class RegularPricing implements PricingStrategy {
    @Override public double calculatePrice(double basePrice) { return basePrice; }
}

public class PremiumPricing implements PricingStrategy {    // 20% discount
    @Override public double calculatePrice(double basePrice) { return basePrice * 0.8; }
}

public class SurgePricing implements PricingStrategy {      // 2x surge
    @Override public double calculatePrice(double basePrice) { return basePrice * 2.0; }
}
```

---

### Observer

**Intent:** When one object changes state, all dependents are notified automatically.

**When to use:** Multiple objects need to react to state changes in another object.

```java
public interface Observer<T> {
    void update(T event);
}

public interface Observable<T> {
    void subscribe(Observer<T> observer);
    void unsubscribe(Observer<T> observer);
    void notifyObservers(T event);
}

// Event types
public record OrderEvent(String orderId, String status) {}

// Observable subject
public class OrderService implements Observable<OrderEvent> {
    private final List<Observer<OrderEvent>> observers = new ArrayList<>();
    private final Map<String, String> orders = new HashMap<>();

    @Override
    public void subscribe(Observer<OrderEvent> observer) {
        observers.add(observer);
    }

    @Override
    public void unsubscribe(Observer<OrderEvent> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(OrderEvent event) {
        for (Observer<OrderEvent> o : observers) {
            o.update(event);
        }
    }

    public void updateStatus(String orderId, String status) {
        orders.put(orderId, status);
        notifyObservers(new OrderEvent(orderId, status));
    }
}

// Concrete observers
public class EmailObserver implements Observer<OrderEvent> {
    @Override
    public void update(OrderEvent event) {
        System.out.println("Email: Order " + event.orderId() + " is now " + event.status());
    }
}

public class InventoryObserver implements Observer<OrderEvent> {
    @Override
    public void update(OrderEvent event) {
        if ("SHIPPED".equals(event.status())) {
            System.out.println("Inventory: Deducting stock for order " + event.orderId());
        }
    }
}

// Usage
OrderService service = new OrderService();
service.subscribe(new EmailObserver());
service.subscribe(new InventoryObserver());

service.updateStatus("ORD-001", "CONFIRMED");
service.updateStatus("ORD-001", "SHIPPED");
```

---

### State Machine

**Intent:** Object behavior changes based on its internal state. Each state is a separate class/object.

**When to use:** An entity goes through well-defined states with specific allowed transitions.

```java
// State interface
public interface OrderState {
    void confirm(OrderContext context);
    void ship(OrderContext context);
    void deliver(OrderContext context);
    void cancel(OrderContext context);
    String getStateName();
}

// Context holds the current state
public class OrderContext {
    private OrderState state;
    private final String orderId;

    public OrderContext(String orderId) {
        this.orderId = orderId;
        this.state = new PendingState();
    }

    public void setState(OrderState state)  { this.state = state; }
    public void confirm()                   { state.confirm(this); }
    public void ship()                      { state.ship(this); }
    public void deliver()                   { state.deliver(this); }
    public void cancel()                    { state.cancel(this); }
    public String getStatus()               { return state.getStateName(); }
}

// Concrete states
public class PendingState implements OrderState {
    @Override public String getStateName() { return "PENDING"; }

    @Override public void confirm(OrderContext ctx) {
        System.out.println("Order confirmed.");
        ctx.setState(new ConfirmedState());
    }

    @Override public void ship(OrderContext ctx) {
        throw new IllegalStateException("Cannot ship a pending order");
    }

    @Override public void deliver(OrderContext ctx) {
        throw new IllegalStateException("Cannot deliver a pending order");
    }

    @Override public void cancel(OrderContext ctx) {
        System.out.println("Order cancelled.");
        ctx.setState(new CancelledState());
    }
}

public class ConfirmedState implements OrderState {
    @Override public String getStateName() { return "CONFIRMED"; }

    @Override public void confirm(OrderContext ctx) {
        throw new IllegalStateException("Already confirmed");
    }

    @Override public void ship(OrderContext ctx) {
        System.out.println("Order shipped.");
        ctx.setState(new ShippedState());
    }

    @Override public void deliver(OrderContext ctx) {
        throw new IllegalStateException("Cannot deliver — not yet shipped");
    }

    @Override public void cancel(OrderContext ctx) {
        System.out.println("Order cancelled.");
        ctx.setState(new CancelledState());
    }
}

public class ShippedState implements OrderState {
    @Override public String getStateName() { return "SHIPPED"; }

    @Override public void confirm(OrderContext ctx) { throw new IllegalStateException("Already shipped"); }
    @Override public void ship(OrderContext ctx)    { throw new IllegalStateException("Already shipped"); }
    @Override public void cancel(OrderContext ctx)  { throw new IllegalStateException("Cannot cancel shipped order"); }

    @Override public void deliver(OrderContext ctx) {
        System.out.println("Order delivered.");
        ctx.setState(new DeliveredState());
    }
}

public class DeliveredState implements OrderState {
    @Override public String getStateName() { return "DELIVERED"; }

    @Override public void confirm(OrderContext ctx) { throw new IllegalStateException("Order complete"); }
    @Override public void ship(OrderContext ctx)    { throw new IllegalStateException("Order complete"); }
    @Override public void deliver(OrderContext ctx) { throw new IllegalStateException("Already delivered"); }
    @Override public void cancel(OrderContext ctx)  { throw new IllegalStateException("Cannot cancel delivered order"); }
}

public class CancelledState implements OrderState {
    @Override public String getStateName() { return "CANCELLED"; }

    @Override public void confirm(OrderContext ctx) { throw new IllegalStateException("Order cancelled"); }
    @Override public void ship(OrderContext ctx)    { throw new IllegalStateException("Order cancelled"); }
    @Override public void deliver(OrderContext ctx) { throw new IllegalStateException("Order cancelled"); }
    @Override public void cancel(OrderContext ctx)  { throw new IllegalStateException("Already cancelled"); }
}

// Usage
OrderContext order = new OrderContext("ORD-001");
order.confirm();   // PENDING → CONFIRMED
order.ship();      // CONFIRMED → SHIPPED
order.deliver();   // SHIPPED → DELIVERED
// order.cancel(); // IllegalStateException — cannot cancel delivered order
```

---

## Pattern Selection Quick Reference

| Scenario | Pattern |
|----------|---------|
| Different algorithms swappable at runtime | **Strategy** |
| Object goes through well-defined states | **State Machine** |
| Create objects without specifying exact class | **Factory** |
| Complex object with many optional params | **Builder** |
| Exactly one global instance needed | **Singleton** |
| Add optional behavior without subclassing | **Decorator** |
| Simplify complex subsystem | **Facade** |
| Multiple objects react to state changes | **Observer** |
| Encapsulate undo/redo operations | **Command** |
| Treat individual and group uniformly | **Composite** |
