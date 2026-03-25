# Online Shopping System

**Difficulty:** Hard | **Companies:** Amazon, Flipkart, Walmart

---

## Requirements

> "Design an online shopping system like Amazon that supports product catalog, cart management, checkout with inventory deduction, and order placement."

### Clarifying Questions

> **You:** "Should we handle concurrent purchases of the same product?"
>
> **Interviewer:** "Yes. Use ReentrantLock on Product for atomic stock deduction."

> **You:** "What's the checkout flow?"
>
> **Interviewer:** "Four phases: apply coupon → deduct inventory → create order → charge payment. Roll back on failure."

> **You:** "How do we handle payment?"
>
> **Interviewer:** "Use a PaymentGateway strategy pattern — the actual processing is abstracted behind an interface."

> **You:** "Should Cart and Order be separate entities?"
>
> **Interviewer:** "Yes. Cart is mutable (add/remove items). Order is immutable once placed."

### Final Requirements

```
Requirements:
1. Product catalog with categories, search by name
2. Cart: add, remove, update quantity, view
3. Product inventory with ReentrantLock for concurrent deduction
4. 4-phase checkout: coupon → deduct stock → create order → payment
5. Rollback on failure at any phase
6. PaymentGateway strategy for payment processing
7. Order with status tracking (PLACED, SHIPPED, DELIVERED, CANCELLED)
8. Coupon / discount support

Out of scope:
- Recommendations / personalization
- Seller management
- Shipping logistics
- Reviews and ratings
- Wishlists
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **ShoppingService** | Facade. Manages products, carts, checkout, orders. |
| **Product** | Catalog item with price, stock, category. ReentrantLock for inventory. |
| **Cart** | Per-user mutable cart. Maps product to quantity. |
| **CartItem** | Product reference + quantity snapshot. |
| **Order** | Immutable record created from cart at checkout. Tracks status. |
| **OrderItem** | Product snapshot (name, price) + quantity. |
| **Coupon** | Code, discount type (flat/percent), minimum order, expiry. |
| **PaymentGateway** | Strategy interface for charging. |
| **User** | Customer profile with shipping address. |

---

## Class Design

### Product

```
class Product:
    - productId: String
    - name: String
    - price: double
    - stock: int
    - category: String
    - lock: ReentrantLock

    + deductStock(qty) → boolean   // under lock
    + restoreStock(qty) → void
```

### Cart

```
class Cart:
    - userId: String
    - items: Map<String, CartItem>  // productId → CartItem

    + addItem(product, qty) → void
    + removeItem(productId) → void
    + updateQuantity(productId, qty) → void
    + getTotal() → double
    + clear() → void
```

### Order

```
class Order:
    - orderId: String
    - userId: String
    - items: List<OrderItem>
    - subtotal: double
    - discount: double
    - total: double
    - status: OrderStatus
    - placedAt: Instant
    - shippingAddress: String
```

### Coupon

```
class Coupon:
    - code: String
    - discountType: DiscountType  // FLAT, PERCENT
    - discountValue: double
    - minOrderAmount: double
    - expiresAt: Instant

    + apply(subtotal) → double  // returns discount amount
    + isValid(subtotal) → boolean
```

### PaymentGateway (Strategy)

```
interface PaymentGateway:
    + charge(userId, amount) → boolean
    + refund(userId, amount) → boolean
```

### ShoppingService

```
class ShoppingService:
    - products: Map<String, Product>
    - carts: Map<String, Cart>
    - orders: Map<String, Order>
    - coupons: Map<String, Coupon>
    - paymentGateway: PaymentGateway

    + addProduct(product) → void
    + searchProducts(keyword) → List<Product>
    + addToCart(userId, productId, qty) → void
    + removeFromCart(userId, productId) → void
    + checkout(userId, couponCode?, address) → Order
    + cancelOrder(orderId) → void
```

---

## Implementation

### Product.deductStock (thread-safe)

```
deductStock(qty)
    lock.lock()
    try:
        if stock < qty → return false
        stock -= qty
        return true
    finally:
        lock.unlock()
```

### ShoppingService.checkout (4-phase with rollback)

```
checkout(userId, couponCode, address)
    cart = carts.get(userId)
    if cart is empty → throw "Cart empty"

    // Phase 1: Apply coupon
    discount = 0
    if couponCode != null:
        coupon = coupons.get(couponCode)
        if !coupon.isValid(cart.getTotal()) → throw "Invalid coupon"
        discount = coupon.apply(cart.getTotal())

    // Phase 2: Deduct inventory
    deducted = []
    for each item in cart:
        if !item.product.deductStock(item.qty):
            // Rollback deducted
            for each d in deducted: d.product.restoreStock(d.qty)
            throw "Out of stock: " + item.product.name
        deducted.add(item)

    // Phase 3: Create order
    total = cart.getTotal() - discount
    order = new Order(userId, cart.items, total, discount, address)

    // Phase 4: Charge payment
    if !paymentGateway.charge(userId, total):
        // Rollback inventory
        for each d in deducted: d.product.restoreStock(d.qty)
        throw "Payment failed"

    orders.put(order.id, order)
    cart.clear()
    return order
```

### Complete Code Implementation

```java
public enum OrderStatus {
    PLACED, SHIPPED, DELIVERED, CANCELLED
}
```

```java
public enum DiscountType {
    FLAT, PERCENT
}
```

```java
import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private final String productId;
    private final String name;
    private double price;
    private int stock;
    private final String category;
    private final ReentrantLock lock = new ReentrantLock();

    public Product(String productId, String name, double price,
                   int stock, String category) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    public boolean deductStock(int qty) {
        lock.lock();
        try {
            if (stock < qty) return false;
            stock -= qty;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void restoreStock(int qty) {
        lock.lock();
        try { stock += qty; }
        finally { lock.unlock(); }
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getCategory() { return category; }
}
```

```java
public class User {
    private final String userId;
    private final String name;
    private final String email;
    private String shippingAddress;

    public User(String userId, String name, String email, String address) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.shippingAddress = address;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String address) { this.shippingAddress = address; }
}
```

```java
public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSubtotal() { return product.getPrice() * quantity; }
}
```

```java
import java.util.*;

public class Cart {
    private final String userId;
    private final Map<String, CartItem> items = new LinkedHashMap<>();

    public Cart(String userId) {
        this.userId = userId;
    }

    public void addItem(Product product, int quantity) {
        CartItem existing = items.get(product.getProductId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(product.getProductId(), new CartItem(product, quantity));
        }
    }

    public void removeItem(String productId) {
        items.remove(productId);
    }

    public void updateQuantity(String productId, int quantity) {
        CartItem item = items.get(productId);
        if (item == null) throw new IllegalArgumentException("Item not in cart");
        if (quantity <= 0) { items.remove(productId); return; }
        item.setQuantity(quantity);
    }

    public double getTotal() {
        return items.values().stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public Collection<CartItem> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    public boolean isEmpty() { return items.isEmpty(); }

    public void clear() { items.clear(); }

    public String getUserId() { return userId; }
}
```

```java
import java.time.Instant;

public class Coupon {
    private final String code;
    private final DiscountType discountType;
    private final double discountValue;
    private final double minOrderAmount;
    private final Instant expiresAt;

    public Coupon(String code, DiscountType type, double value,
                  double minOrder, Instant expiresAt) {
        this.code = code;
        this.discountType = type;
        this.discountValue = value;
        this.minOrderAmount = minOrder;
        this.expiresAt = expiresAt;
    }

    public boolean isValid(double subtotal) {
        return subtotal >= minOrderAmount && Instant.now().isBefore(expiresAt);
    }

    public double apply(double subtotal) {
        if (!isValid(subtotal)) return 0;
        if (discountType == DiscountType.FLAT) {
            return Math.min(discountValue, subtotal);
        } else {
            return Math.round(subtotal * discountValue / 100.0 * 100.0) / 100.0;
        }
    }

    public String getCode() { return code; }
}
```

```java
public class OrderItem {
    private final String productId;
    private final String productName;
    private final double priceAtPurchase;
    private final int quantity;

    public OrderItem(CartItem cartItem) {
        this.productId = cartItem.getProduct().getProductId();
        this.productName = cartItem.getProduct().getName();
        this.priceAtPurchase = cartItem.getProduct().getPrice();
        this.quantity = cartItem.getQuantity();
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getPriceAtPurchase() { return priceAtPurchase; }
    public int getQuantity() { return quantity; }
    public double getSubtotal() { return priceAtPurchase * quantity; }
}
```

```java
import java.time.Instant;
import java.util.*;

public class Order {
    private final String orderId;
    private final String userId;
    private final List<OrderItem> items;
    private final double subtotal;
    private final double discount;
    private final double total;
    private OrderStatus status;
    private final Instant placedAt;
    private final String shippingAddress;

    public Order(String orderId, String userId, Collection<CartItem> cartItems,
                 double discount, String shippingAddress) {
        this.orderId = orderId;
        this.userId = userId;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            orderItems.add(new OrderItem(ci));
        }
        this.items = Collections.unmodifiableList(orderItems);
        this.subtotal = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        this.discount = discount;
        this.total = subtotal - discount;
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
        this.shippingAddress = shippingAddress;
    }

    public void ship() {
        if (status != OrderStatus.PLACED)
            throw new IllegalStateException("Cannot ship from: " + status);
        status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        if (status != OrderStatus.SHIPPED)
            throw new IllegalStateException("Cannot deliver from: " + status);
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED)
            throw new IllegalStateException("Cannot cancel from: " + status);
        status = OrderStatus.CANCELLED;
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public Instant getPlacedAt() { return placedAt; }
}
```

```java
public interface PaymentGateway {
    boolean charge(String userId, double amount);
    boolean refund(String userId, double amount);
}
```

```java
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public boolean charge(String userId, double amount) {
        return true; // always succeeds in mock
    }

    @Override
    public boolean refund(String userId, double amount) {
        return true;
    }
}
```

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ShoppingService {
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Cart> carts = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, Coupon> coupons = new ConcurrentHashMap<>();
    private final PaymentGateway paymentGateway;
    private int orderCounter = 0;

    public ShoppingService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void addProduct(Product product) {
        products.put(product.getProductId(), product);
    }

    public void addCoupon(Coupon coupon) {
        coupons.put(coupon.getCode(), coupon);
    }

    public List<Product> searchProducts(String keyword) {
        String lower = keyword.toLowerCase();
        return products.values().stream()
            .filter(p -> p.getName().toLowerCase().contains(lower)
                      || p.getCategory().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public void addToCart(String userId, String productId, int qty) {
        Product product = products.get(productId);
        if (product == null)
            throw new IllegalArgumentException("Product not found");
        Cart cart = carts.computeIfAbsent(userId, Cart::new);
        cart.addItem(product, qty);
    }

    public void removeFromCart(String userId, String productId) {
        Cart cart = carts.get(userId);
        if (cart != null) cart.removeItem(productId);
    }

    public Cart getCart(String userId) {
        return carts.computeIfAbsent(userId, Cart::new);
    }

    public Order checkout(String userId, String couponCode,
                          String shippingAddress) {
        Cart cart = carts.get(userId);
        if (cart == null || cart.isEmpty())
            throw new IllegalStateException("Cart is empty");

        // Phase 1: Apply coupon
        double discount = 0;
        if (couponCode != null) {
            Coupon coupon = coupons.get(couponCode);
            if (coupon == null || !coupon.isValid(cart.getTotal()))
                throw new IllegalArgumentException("Invalid coupon");
            discount = coupon.apply(cart.getTotal());
        }

        // Phase 2: Deduct inventory
        List<CartItem> deducted = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            if (!ci.getProduct().deductStock(ci.getQuantity())) {
                // Rollback
                for (CartItem d : deducted) {
                    d.getProduct().restoreStock(d.getQuantity());
                }
                throw new IllegalStateException(
                    "Out of stock: " + ci.getProduct().getName());
            }
            deducted.add(ci);
        }

        // Phase 3: Create order
        String orderId = "ORD-" + (++orderCounter);
        Order order = new Order(orderId, userId, cart.getItems(),
                                discount, shippingAddress);

        // Phase 4: Charge payment
        if (!paymentGateway.charge(userId, order.getTotal())) {
            // Rollback inventory
            for (CartItem d : deducted) {
                d.getProduct().restoreStock(d.getQuantity());
            }
            throw new IllegalStateException("Payment failed");
        }

        orders.put(orderId, order);
        cart.clear();
        return order;
    }

    public void cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null)
            throw new IllegalArgumentException("Order not found");

        order.cancel();

        // Restore stock
        for (OrderItem oi : order.getItems()) {
            Product product = products.get(oi.getProductId());
            if (product != null) product.restoreStock(oi.getQuantity());
        }

        // Refund
        paymentGateway.refund(order.getUserId(), order.getTotal());
    }

    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }
}
```

### Verification

```
Setup:
  Product: Laptop ($999, stock=5, "Electronics")
  Product: Mouse ($25, stock=50, "Electronics")
  Coupon: "SAVE10" → 10% off, min $50, valid.
  User U1. PaymentGateway = MockPaymentGateway (always succeeds).

Step 1: addToCart("U1", "laptop", 1) → Cart: {laptop:1}
Step 2: addToCart("U1", "mouse", 2) → Cart: {laptop:1, mouse:2}
        cart.getTotal() = 999 + 25*2 = $1049 ✓

Step 3: checkout("U1", "SAVE10", "123 Main St")
  Phase 1: coupon "SAVE10" valid (1049 ≥ 50). discount = 1049 * 10% = $104.90
  Phase 2: Laptop.deductStock(1): 5→4 ✓. Mouse.deductStock(2): 50→48 ✓
  Phase 3: Order created. subtotal=$1049, discount=$104.90, total=$944.10
  Phase 4: paymentGateway.charge("U1", 944.10) → true ✓
  Cart cleared. Order ORD-1 status=PLACED ✓

Step 4: Cart is now empty. checkout again → throws "Cart is empty" ✓

Step 5: cancelOrder("ORD-1")
  order.cancel(). Laptop stock 4→5. Mouse stock 48→50.
  paymentGateway.refund("U1", 944.10) → true ✓. Status=CANCELLED ✓

Step 6: Concurrent stock test — two threads checkout Laptop (stock=5, each wants 3)
  Thread A: Laptop.lock → stock 5≥3 → deduct → stock=2 → unlock ✓
  Thread B: Laptop.lock → stock 2<3 → return false → rollback ✓
```

---

## Extensibility

### 1. "How would you add a wishlist?"

> "I'd create a Wishlist class per user, similar to Cart but without quantities. It stores a Set of product IDs. Users can move items from wishlist to cart. The wishlist doesn't interact with inventory — stock is only checked at checkout."

### 2. "How would you add multiple payment methods?"

> "The PaymentGateway strategy already supports this. I'd create implementations for CreditCardGateway, UPIGateway, WalletGateway, etc. At checkout, the user selects a payment method, and we route to the appropriate gateway. A CompositeGateway could split payment across two methods."

### 3. "How would you handle flash sales with high concurrency?"

> "For flash sales, I'd pre-allocate inventory into a Redis-like atomic counter for O(1) decrement. Requests are queued and processed in order. A token bucket limits request rate per user. The product page shows a pessimistic stock count, and oversold checks happen at payment confirmation as a safety net."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model Product, Cart, and Order correctly with the cart-to-order conversion. Adding and removing items from the cart, computing totals, and placing a basic order are the key goals. Inventory deduction and payment are not required.

### Mid-level

Mid-level candidates should implement the 4-phase checkout with rollback — coupon validation, stock deduction with ReentrantLock, order creation, and payment via a strategy interface. Coupon support with flat/percent discounts, and proper error handling when stock runs out mid-checkout (partial rollback), should be demonstrated.

### Senior

Senior candidates would discuss distributed inventory management (optimistic locking, eventual consistency), idempotent checkout APIs, saga patterns for multi-step transactions across microservices, and how to handle payment failures after inventory deduction. Cart-to-order race conditions, double-charge prevention, and cache-aside patterns for product catalog reads at scale are key discussion points.