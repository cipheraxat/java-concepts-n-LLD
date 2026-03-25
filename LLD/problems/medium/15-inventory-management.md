# Inventory Management System

**Difficulty:** Medium | **Companies:** Amazon, Walmart, Flipkart

---

## Requirements

> "Design the backend for an inventory management system. Track products across multiple warehouses with stock levels, support reserve/confirm/release flow for orders, and notify on low stock."

### Clarifying Questions

> **You:** "Single warehouse or multiple?"
>
> **Interviewer:** "Multiple warehouses. Each product can exist in multiple warehouses with different quantities."

> **You:** "How does the order flow interact with inventory?"
>
> **Interviewer:** "Three-phase: reserve stock (soft lock), confirm (deduct), or release (cancel reservation). This prevents overselling."

> **You:** "Low-stock notifications?"
>
> **Interviewer:** "When stock for a product in a warehouse drops below a configurable threshold, notify observers."

> **You:** "Do we need product categories or pricing?"
>
> **Interviewer:** "No. Focus on stock tracking, reservation flow, and notifications."

### Final Requirements

```
Requirements:
1. Multiple warehouses, each holding inventory items (product + quantity)
2. Reserve stock: soft-lock a quantity for an order
3. Confirm reservation: permanently deduct the reserved quantity
4. Release reservation: return reserved quantity back to available stock
5. Observer pattern: notify when stock drops below a threshold
6. Thread-safe stock operations

Out of scope:
- Product pricing / categories
- Order management (handled by a separate system)
- Shipping / logistics
- Purchase orders / restocking automation
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **InventoryService** | Facade. Coordinates reserve/confirm/release across warehouses. |
| **Warehouse** | Holds a collection of InventoryItems keyed by product. |
| **Product** | Product metadata (SKU, name). Immutable identity. |
| **InventoryItem** | Tracks available and reserved quantities for one product in one warehouse. Owns a ReentrantLock for thread safety. |
| **Reservation** | Links a reservation ID to a product, warehouse, and reserved quantity. |
| **StockObserver** | Interface for low-stock notifications. |

---

## Class Design

### InventoryItem

| Requirement | What InventoryItem must track |
|-------------|------------------------------|
| "Reserve / confirm / release" | available quantity + reserved quantity |
| "Thread-safe" | ReentrantLock per item |
| "Low-stock threshold" | threshold + list of observers |

```
class InventoryItem:
    - product: Product
    - warehouse: Warehouse
    - availableQty: int
    - reservedQty: int
    - lowStockThreshold: int
    - observers: List<StockObserver>
    - lock: ReentrantLock

    + reserve(qty) → boolean
    + confirmReservation(qty) → void
    + releaseReservation(qty) → void
    + restock(qty) → void
    + getAvailableQty() → int
```

### InventoryService

```
class InventoryService:
    - warehouses: Map<String, Warehouse>
    - reservations: Map<String, Reservation>
    - reservationCounter: AtomicLong

    + reserveStock(productId, warehouseId, qty) → Optional<Reservation>
    + confirmReservation(reservationId) → boolean
    + releaseReservation(reservationId) → boolean
    + checkStock(productId, warehouseId) → int
    + restock(productId, warehouseId, qty) → void
```

### Final Class Design

```
class InventoryService: facade
class Warehouse: warehouseId, name, Map<productId, InventoryItem>
class Product: productId, name, sku
class InventoryItem: product, availableQty, reservedQty, lock, observers
class Reservation: reservationId, product, warehouse, qty
interface StockObserver: onLowStock(product, warehouse, currentQty)
```

---

## Implementation

### InventoryItem.reserve

**Core logic:** Lock, check available >= requested, deduct from available, add to reserved, check threshold.

```
reserve(qty)
    lock.lock()
    try:
        if availableQty < qty → return false
        availableQty -= qty
        reservedQty += qty
        checkThreshold()
        return true
    finally:
        lock.unlock()
```

### InventoryItem.confirmReservation

```
confirmReservation(qty)
    lock.lock()
    try:
        reservedQty -= qty
        // Stock is permanently consumed — no change to availableQty
    finally:
        lock.unlock()
```

### InventoryItem.releaseReservation

```
releaseReservation(qty)
    lock.lock()
    try:
        reservedQty -= qty
        availableQty += qty  // Return to available pool
    finally:
        lock.unlock()
```

### Complete Code Implementation

```java
public class Product {
    private final String productId;
    private final String name;
    private final String sku;

    public Product(String productId, String name, String sku) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
    }

    public String getProductId() { return productId; }
    public String getName()      { return name; }
    public String getSku()       { return sku; }
}
```

```java
public interface StockObserver {
    void onLowStock(Product product, String warehouseId, int currentQty);
}

public class ConsoleStockObserver implements StockObserver {
    @Override
    public void onLowStock(Product product, String warehouseId, int currentQty) {
        System.out.printf("[LOW STOCK] %s in warehouse %s: %d remaining%n",
            product.getName(), warehouseId, currentQty);
    }
}
```

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class InventoryItem {
    private final Product product;
    private final String warehouseId;
    private int availableQty;
    private int reservedQty;
    private int lowStockThreshold;
    private final List<StockObserver> observers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public InventoryItem(Product product, String warehouseId,
                         int initialQty, int lowStockThreshold) {
        this.product = product;
        this.warehouseId = warehouseId;
        this.availableQty = initialQty;
        this.reservedQty = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean reserve(int qty) {
        lock.lock();
        try {
            if (availableQty < qty) return false;
            availableQty -= qty;
            reservedQty += qty;
            checkThreshold();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void confirmReservation(int qty) {
        lock.lock();
        try {
            reservedQty -= qty;
        } finally {
            lock.unlock();
        }
    }

    public void releaseReservation(int qty) {
        lock.lock();
        try {
            reservedQty -= qty;
            availableQty += qty;
        } finally {
            lock.unlock();
        }
    }

    public void restock(int qty) {
        lock.lock();
        try {
            availableQty += qty;
        } finally {
            lock.unlock();
        }
    }

    private void checkThreshold() {
        if (availableQty <= lowStockThreshold) {
            for (StockObserver obs : observers) {
                obs.onLowStock(product, warehouseId, availableQty);
            }
        }
    }

    public void addObserver(StockObserver observer) { observers.add(observer); }

    public int getAvailableQty() {
        lock.lock();
        try { return availableQty; }
        finally { lock.unlock(); }
    }

    public int getReservedQty() {
        lock.lock();
        try { return reservedQty; }
        finally { lock.unlock(); }
    }

    public Product getProduct() { return product; }
}
```

```java
public class Reservation {
    private final String reservationId;
    private final Product product;
    private final String warehouseId;
    private final int quantity;

    public Reservation(String reservationId, Product product,
                       String warehouseId, int quantity) {
        this.reservationId = reservationId;
        this.product = product;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public String getReservationId() { return reservationId; }
    public Product getProduct()      { return product; }
    public String getWarehouseId()   { return warehouseId; }
    public int getQuantity()         { return quantity; }
}
```

```java
import java.util.HashMap;
import java.util.Map;

public class Warehouse {
    private final String warehouseId;
    private final String name;
    private final Map<String, InventoryItem> items = new HashMap<>();

    public Warehouse(String warehouseId, String name) {
        this.warehouseId = warehouseId;
        this.name = name;
    }

    public void addItem(InventoryItem item) {
        items.put(item.getProduct().getProductId(), item);
    }

    public InventoryItem getItem(String productId) {
        return items.get(productId);
    }

    public String getWarehouseId() { return warehouseId; }
}
```

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InventoryService {
    private final Map<String, Warehouse> warehouses = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final AtomicLong reservationCounter = new AtomicLong(1);

    public void addWarehouse(Warehouse warehouse) {
        warehouses.put(warehouse.getWarehouseId(), warehouse);
    }

    public Optional<Reservation> reserveStock(String productId,
                                               String warehouseId, int qty) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) return Optional.empty();

        InventoryItem item = warehouse.getItem(productId);
        if (item == null) return Optional.empty();

        if (!item.reserve(qty)) return Optional.empty();

        Reservation reservation = new Reservation(
            "RSV-" + reservationCounter.getAndIncrement(),
            item.getProduct(), warehouseId, qty);
        reservations.put(reservation.getReservationId(), reservation);
        return Optional.of(reservation);
    }

    public boolean confirmReservation(String reservationId) {
        Reservation res = reservations.remove(reservationId);
        if (res == null) return false;

        Warehouse warehouse = warehouses.get(res.getWarehouseId());
        InventoryItem item = warehouse.getItem(res.getProduct().getProductId());
        item.confirmReservation(res.getQuantity());
        return true;
    }

    public boolean releaseReservation(String reservationId) {
        Reservation res = reservations.remove(reservationId);
        if (res == null) return false;

        Warehouse warehouse = warehouses.get(res.getWarehouseId());
        InventoryItem item = warehouse.getItem(res.getProduct().getProductId());
        item.releaseReservation(res.getQuantity());
        return true;
    }

    public int checkStock(String productId, String warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) return 0;
        InventoryItem item = warehouse.getItem(productId);
        return item != null ? item.getAvailableQty() : 0;
    }

    public void restock(String productId, String warehouseId, int qty) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) return;
        InventoryItem item = warehouse.getItem(productId);
        if (item != null) item.restock(qty);
    }
}
```

### Verification

```
Setup: Warehouse W1 with Product "Laptop" (initial: 10, threshold: 3).
Observer: ConsoleStockObserver.

Step 1: Reserve 4 Laptops
  item.reserve(4): available=10 >= 4 ✓
  available: 10→6, reserved: 0→4
  checkThreshold: 6 > 3 → no alert
  Reservation RSV-1 created.

Step 2: Reserve 4 more Laptops
  item.reserve(4): available=6 >= 4 ✓
  available: 6→2, reserved: 4→8
  checkThreshold: 2 ≤ 3 → [LOW STOCK] Laptop in W1: 2 remaining

Step 3: Confirm RSV-1
  item.confirmReservation(4): reserved: 8→4
  available stays 2 (stock permanently consumed)

Step 4: Release RSV-2
  item.releaseReservation(4): reserved: 4→0, available: 2→6
  Stock returned to available pool.

Step 5: Reserve 7 Laptops
  item.reserve(7): available=6 < 7 → return false
  No reservation created. ✓

Step 6: checkStock → 6 available
```

---

## Extensibility

### 1. "How would you support multi-warehouse reservation?"

> "For an order needing 10 units when W1 has 6 and W2 has 5, I'd add a `reserveAcrossWarehouses(productId, qty)` method that tries warehouses in priority order, reserving partial quantities from each. It returns a list of Reservations. If the total can't be met, it releases all partial reservations — all-or-nothing semantics."

### 2. "How would you add reservation expiry?"

> "Each Reservation gets a `createdAt` timestamp and a configurable TTL. A `ScheduledExecutorService` periodically scans and releases expired reservations. This prevents stock from being soft-locked indefinitely when the order service crashes."

### 3. "How would you add batch restocking with audit trail?"

> "I'd create a `RestockEvent` record (product, warehouse, qty, timestamp, source). The `restock` method publishes to an event log. A `RestockHistory` can be queried for audit purposes. Batch operations wrap multiple restocks in a single transaction-like unit."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model Product, Warehouse, and a basic stock tracker that supports adding and deducting quantities. The reserve/confirm/release pattern isn't required — a simple deduction on order is fine. Thread safety and observers aren't expected.

### Mid-level

Mid-level candidates should implement the three-phase reserve/confirm/release flow with proper separation between available and reserved quantities. ReentrantLock per InventoryItem for thread safety is expected. The Observer pattern for low-stock alerts should be in place with a clean interface.

### Senior

Senior candidates would discuss multi-warehouse reservation with all-or-nothing semantics, reservation expiry via scheduled cleanup, and the interaction between inventory and order services. You'd explain how to prevent double-confirmation and how the system recovers if the order service crashes mid-flow (idempotent confirmations, timeout-based auto-release).