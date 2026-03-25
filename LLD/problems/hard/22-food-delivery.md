# Food Delivery System

**Difficulty:** Hard | **Companies:** Uber Eats, DoorDash, Swiggy, Zomato

---

## Requirements

> "Design a food delivery system that lets customers browse restaurants, place orders, and get food delivered by delivery agents."

### Clarifying Questions

> **You:** "Three parties — customer, restaurant, delivery agent?"
>
> **Interviewer:** "Yes. Focus on order lifecycle and agent assignment."

> **You:** "Do restaurants manage their own menus and inventory?"
>
> **Interviewer:** "Yes. Each MenuItem has a stock count. Deduct on order, roll back on cancellation."

> **You:** "How is the delivery agent assigned?"
>
> **Interviewer:** "Nearest available agent with tryAssign (ReentrantLock), similar to ride sharing."

> **You:** "Should we use Observer for order status changes?"
>
> **Interviewer:** "Yes — notify customer and restaurant on each status transition."

### Final Requirements

```
Requirements:
1. Restaurant with menu items and stock management
2. Customer places order (multiple items from one restaurant)
3. Order state machine: PLACED → CONFIRMED → PREPARING → READY → PICKED_UP → DELIVERED / CANCELLED
4. Delivery agent assignment: nearest available, tryAssign with ReentrantLock
5. Observer pattern: notify customer and restaurant on status changes
6. MenuItem inventory: deduct on order, rollback on cancel
7. Price calculation: sum of (item price × quantity)

Out of scope:
- Payment processing
- Restaurant search / filtering
- Delivery route optimization
- Ratings and reviews
- Promo codes / discounts
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **DeliveryService** | Facade. Coordinates order placement, agent assignment, status transitions. |
| **Customer** | Holds customer info and delivery address. |
| **Restaurant** | Holds menu, location. Manages MenuItem stock. |
| **MenuItem** | Name, price, stock count. Thread-safe deduction. |
| **Order** | State machine. Tracks items, customer, restaurant, agent, timestamps. |
| **OrderItem** | MenuItem reference + quantity. |
| **DeliveryAgent** | Location, availability, ReentrantLock for tryAssign. |
| **OrderObserver** | Interface for status change notifications. |
| **OrderStatus** | Enum for the order state machine. |

---

## Class Design

### MenuItem

```
class MenuItem:
    - itemId: String
    - name: String
    - price: double
    - stock: AtomicInteger

    + deductStock(qty) → boolean
    + restoreStock(qty) → void
```

### Restaurant

```
class Restaurant:
    - restaurantId: String
    - name: String
    - location: Location
    - menu: Map<String, MenuItem>

    + getMenuItem(itemId) → MenuItem
    + getMenu() → List<MenuItem>
```

### Order

```
class Order:
    - orderId: String
    - customer: Customer
    - restaurant: Restaurant
    - items: List<OrderItem>
    - agent: DeliveryAgent?
    - status: OrderStatus
    - totalPrice: double
    - observers: List<OrderObserver>
    - placedAt, deliveredAt: Instant?

    + confirm() → void
    + markPreparing() → void
    + markReady() → void
    + pickUp() → void
    + deliver() → void
    + cancel() → void
    + addObserver(observer) → void
    - notifyObservers() → void
```

### DeliveryAgent

```
class DeliveryAgent:
    - agentId: String
    - name: String
    - location: Location
    - available: boolean
    - lock: ReentrantLock

    + tryAssign(order) → boolean
    + release() → void
```

### DeliveryService

```
class DeliveryService:
    - restaurants: Map<String, Restaurant>
    - agents: List<DeliveryAgent>
    - orders: Map<String, Order>
    - maxRadiusKm: double

    + placeOrder(customer, restaurantId, items) → Order
    + confirmOrder(orderId) → void
    + markPreparing(orderId) → void
    + markReady(orderId) → void
    + pickUpOrder(orderId) → void
    + deliverOrder(orderId) → void
    + cancelOrder(orderId) → void
    - assignAgent(order) → DeliveryAgent
```

---

## Implementation

### MenuItem.deductStock (atomic)

```
deductStock(qty)
    while true:
        current = stock.get()
        if current < qty → return false
        if stock.compareAndSet(current, current - qty) → return true
```

### Order state transitions (with Observer)

```
confirm()
    assert status == PLACED
    status = CONFIRMED
    notifyObservers()

cancel()
    assert status not in [DELIVERED, CANCELLED]
    status = CANCELLED
    // rollback stock
    for each item in items:
        item.menuItem.restoreStock(item.quantity)
    if agent != null → agent.release()
    notifyObservers()
```

### DeliveryService.placeOrder

```
placeOrder(customer, restaurantId, items: List<(itemId, qty)>)
    restaurant = restaurants.get(restaurantId)
    orderItems = []
    // Phase 1: deduct stock
    for each (itemId, qty) in items:
        menuItem = restaurant.getMenuItem(itemId)
        if !menuItem.deductStock(qty):
            // rollback already-deducted
            for each deducted item: restoreStock
            throw "Out of stock"
        orderItems.add(new OrderItem(menuItem, qty))

    order = new Order(customer, restaurant, orderItems)
    orders.put(order.id, order)
    return order
```

### Complete Code Implementation

```java
public class Location {
    private final double latitude;
    private final double longitude;
    private static final double EARTH_RADIUS_KM = 6371.0;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double distanceTo(Location other) {
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(this.latitude))
            * Math.cos(Math.toRadians(other.latitude))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
```

```java
public enum OrderStatus {
    PLACED, CONFIRMED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED
}
```

```java
public class Customer {
    private final String customerId;
    private final String name;
    private final Location deliveryAddress;

    public Customer(String customerId, String name, Location deliveryAddress) {
        this.customerId = customerId;
        this.name = name;
        this.deliveryAddress = deliveryAddress;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public Location getDeliveryAddress() { return deliveryAddress; }
}
```

```java
import java.util.concurrent.atomic.AtomicInteger;

public class MenuItem {
    private final String itemId;
    private final String name;
    private final double price;
    private final AtomicInteger stock;

    public MenuItem(String itemId, String name, double price, int stock) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.stock = new AtomicInteger(stock);
    }

    public boolean deductStock(int qty) {
        while (true) {
            int current = stock.get();
            if (current < qty) return false;
            if (stock.compareAndSet(current, current - qty)) return true;
        }
    }

    public void restoreStock(int qty) {
        stock.addAndGet(qty);
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock.get(); }
}
```

```java
public class OrderItem {
    private final MenuItem menuItem;
    private final int quantity;

    public OrderItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public double getSubtotal() { return menuItem.getPrice() * quantity; }
}
```

```java
import java.util.*;

public class Restaurant {
    private final String restaurantId;
    private final String name;
    private final Location location;
    private final Map<String, MenuItem> menu = new LinkedHashMap<>();

    public Restaurant(String restaurantId, String name, Location location) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.location = location;
    }

    public void addMenuItem(MenuItem item) {
        menu.put(item.getItemId(), item);
    }

    public MenuItem getMenuItem(String itemId) {
        MenuItem item = menu.get(itemId);
        if (item == null)
            throw new IllegalArgumentException("Menu item not found: " + itemId);
        return item;
    }

    public List<MenuItem> getMenu() { return new ArrayList<>(menu.values()); }

    public String getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
}
```

```java
import java.util.concurrent.locks.ReentrantLock;

public class DeliveryAgent {
    private final String agentId;
    private final String name;
    private Location location;
    private boolean available;
    private final ReentrantLock lock = new ReentrantLock();

    public DeliveryAgent(String agentId, String name, Location location) {
        this.agentId = agentId;
        this.name = name;
        this.location = location;
        this.available = true;
    }

    public boolean tryAssign(Order order) {
        lock.lock();
        try {
            if (!available) return false;
            available = false;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void release() {
        lock.lock();
        try { available = true; }
        finally { lock.unlock(); }
    }

    public String getAgentId() { return agentId; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public boolean isAvailable() { return available; }
    public void updateLocation(Location location) { this.location = location; }
}
```

```java
public interface OrderObserver {
    void onStatusChange(Order order, OrderStatus oldStatus, OrderStatus newStatus);
}
```

```java
import java.time.Instant;
import java.util.*;

public class Order {
    private final String orderId;
    private final Customer customer;
    private final Restaurant restaurant;
    private final List<OrderItem> items;
    private DeliveryAgent agent;
    private OrderStatus status;
    private final double totalPrice;
    private final Instant placedAt;
    private Instant deliveredAt;
    private final List<OrderObserver> observers = new ArrayList<>();

    public Order(String orderId, Customer customer, Restaurant restaurant,
                 List<OrderItem> items) {
        this.orderId = orderId;
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = Collections.unmodifiableList(items);
        this.totalPrice = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
    }

    public void addObserver(OrderObserver observer) {
        observers.add(observer);
    }

    private void transition(OrderStatus expected, OrderStatus next) {
        if (this.status != expected)
            throw new IllegalStateException(
                "Cannot transition from " + status + " to " + next);
        OrderStatus old = this.status;
        this.status = next;
        notifyObservers(old, next);
    }

    public void confirm() { transition(OrderStatus.PLACED, OrderStatus.CONFIRMED); }
    public void markPreparing() { transition(OrderStatus.CONFIRMED, OrderStatus.PREPARING); }
    public void markReady() { transition(OrderStatus.PREPARING, OrderStatus.READY); }

    public void pickUp() {
        transition(OrderStatus.READY, OrderStatus.PICKED_UP);
    }

    public void deliver() {
        transition(OrderStatus.PICKED_UP, OrderStatus.DELIVERED);
        this.deliveredAt = Instant.now();
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED)
            throw new IllegalStateException("Cannot cancel in state: " + status);
        OrderStatus old = this.status;
        this.status = OrderStatus.CANCELLED;
        // Rollback stock
        for (OrderItem item : items) {
            item.getMenuItem().restoreStock(item.getQuantity());
        }
        if (agent != null) agent.release();
        notifyObservers(old, OrderStatus.CANCELLED);
    }

    public void assignAgent(DeliveryAgent agent) { this.agent = agent; }

    private void notifyObservers(OrderStatus oldStatus, OrderStatus newStatus) {
        for (OrderObserver obs : observers) {
            obs.onStatusChange(this, oldStatus, newStatus);
        }
    }

    public String getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public Restaurant getRestaurant() { return restaurant; }
    public List<OrderItem> getItems() { return items; }
    public DeliveryAgent getAgent() { return agent; }
    public OrderStatus getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
```

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DeliveryService {
    private final Map<String, Restaurant> restaurants = new ConcurrentHashMap<>();
    private final List<DeliveryAgent> agents = new ArrayList<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final double maxRadiusKm;
    private int orderCounter = 0;

    public DeliveryService(double maxRadiusKm) {
        this.maxRadiusKm = maxRadiusKm;
    }

    public void addRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.getRestaurantId(), restaurant);
    }

    public void addAgent(DeliveryAgent agent) {
        agents.add(agent);
    }

    public Order placeOrder(Customer customer, String restaurantId,
                            Map<String, Integer> itemQuantities) {
        Restaurant restaurant = restaurants.get(restaurantId);
        if (restaurant == null)
            throw new IllegalArgumentException("Restaurant not found");

        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItem> deducted = new ArrayList<>();

        // Phase 1: deduct stock with rollback on failure
        for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            MenuItem item = restaurant.getMenuItem(entry.getKey());
            int qty = entry.getValue();
            if (!item.deductStock(qty)) {
                // Rollback
                for (OrderItem d : deducted) {
                    d.getMenuItem().restoreStock(d.getQuantity());
                }
                throw new IllegalStateException("Out of stock: " + item.getName());
            }
            OrderItem oi = new OrderItem(item, qty);
            orderItems.add(oi);
            deducted.add(oi);
        }

        String orderId = "ORD-" + (++orderCounter);
        Order order = new Order(orderId, customer, restaurant, orderItems);
        orders.put(orderId, order);
        return order;
    }

    public void confirmOrder(String orderId) {
        Order order = getOrder(orderId);
        order.confirm();
        // Assign agent on confirmation
        DeliveryAgent agent = assignAgent(order);
        order.assignAgent(agent);
    }

    public void markPreparing(String orderId) {
        getOrder(orderId).markPreparing();
    }

    public void markReady(String orderId) {
        getOrder(orderId).markReady();
    }

    public void pickUpOrder(String orderId) {
        getOrder(orderId).pickUp();
    }

    public void deliverOrder(String orderId) {
        Order order = getOrder(orderId);
        order.deliver();
        order.getAgent().release();
    }

    public void cancelOrder(String orderId) {
        getOrder(orderId).cancel();
    }

    private DeliveryAgent assignAgent(Order order) {
        Location restaurantLoc = order.getRestaurant().getLocation();

        List<DeliveryAgent> candidates = agents.stream()
            .filter(DeliveryAgent::isAvailable)
            .filter(a -> a.getLocation().distanceTo(restaurantLoc) <= maxRadiusKm)
            .sorted(Comparator.comparingDouble(
                a -> a.getLocation().distanceTo(restaurantLoc)))
            .collect(Collectors.toList());

        for (DeliveryAgent candidate : candidates) {
            if (candidate.tryAssign(order)) return candidate;
        }
        throw new IllegalStateException("No delivery agent available");
    }

    private Order getOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null)
            throw new IllegalArgumentException("Order not found: " + orderId);
        return order;
    }
}
```

### Verification

```
Setup:
  Restaurant R1 at (12.97, 77.59). Menu: Burger ($8, stock=10), Fries ($4, stock=20).
  Agent A1 at (12.975, 77.595) — 0.7km from R1. Agent A2 at (12.98, 77.60) — 1.5km.
  Customer C1 at (12.99, 77.61). maxRadius=5km.

Step 1: placeOrder(C1, R1, {Burger:2, Fries:3})
  Burger.deductStock(2): 10→8 ✓
  Fries.deductStock(3): 20→17 ✓
  totalPrice = 8*2 + 4*3 = 16 + 12 = $28
  Order ORD-1, status=PLACED ✓

Step 2: confirmOrder("ORD-1")
  status PLACED→CONFIRMED. assignAgent:
  A1 is 0.7km (closest, available). A1.tryAssign → true.
  order.agent = A1 ✓

Step 3: markPreparing("ORD-1") → CONFIRMED→PREPARING ✓

Step 4: markReady("ORD-1") → PREPARING→READY ✓

Step 5: pickUpOrder("ORD-1") → READY→PICKED_UP ✓

Step 6: deliverOrder("ORD-1") → PICKED_UP→DELIVERED. A1.release() ✓

Step 7: cancelOrder — on a new order ORD-2 (status PLACED)
  Burger stock 8→10 (restored). Fries stock 17→20 (restored).
  status=CANCELLED. No agent to release ✓

Step 8: placeOrder with Burger qty=15 (only 10 in stock)
  deductStock(15) fails. → throws "Out of stock: Burger" ✓
```

---

## Extensibility

### 1. "How would you add estimated delivery time?"

> "I'd compute ETA as restaurant prep time (average per restaurant or per order size) plus travel time from restaurant to customer. Travel time uses the Haversine distance divided by average speed. The ETA updates as the order progresses through states and is pushed to the customer via the Observer."

### 2. "How would you add restaurant search and filtering?"

> "I'd build a SearchService that indexes restaurants by cuisine type, location (spatial index), rating, and name. Customers search by keyword or filter by cuisine and radius. Results are sorted by relevance, distance, or rating. A spatial index (GeoHash grid) handles the location filtering efficiently."

### 3. "How would you handle multiple restaurants in a single order?"

> "Each restaurant's items become a sub-order. The system deducts stock from each restaurant independently. Multiple delivery agents may be assigned (one per restaurant), or a single agent picks up from multiple locations. The parent order tracks when all sub-orders are delivered."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model Restaurant with a menu, Customer, Order with items, and implement the order state machine (PLACED through DELIVERED). Computing the total price and basic agent assignment are sufficient. Stock management and concurrency are not required.

### Mid-level

Mid-level candidates should implement AtomicInteger for menu item stock with CAS-based deduction, rollback on out-of-stock, DeliveryAgent with tryAssign using ReentrantLock, and the Observer pattern for status notifications. The state machine should enforce valid transitions and reject invalid ones.

### Senior

Senior candidates would discuss eventual consistency across restaurant inventory and order service in a distributed setting, how to handle agent reassignment when an agent goes offline, and compensation patterns for partial failures. Queue-based order processing, circuit breakers for restaurant service calls, and CQRS for separating the order command path from the customer-facing read path demonstrate production-level thinking.