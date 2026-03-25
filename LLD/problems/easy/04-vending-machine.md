# Vending Machine

**Difficulty:** Easy | **Companies:** Google, Microsoft, Amazon

---

## Requirements

As the interview begins, we'll likely be greeted by a simple prompt to set the stage for the architecture we need to design.

> "Design the object-oriented backend for a vending machine. Customers insert money, select a product, and the machine dispenses the item and returns change."

Before jumping into class design, you should ask questions of your interviewer. The goal here is to turn that vague prompt into a concrete specification — something you can actually build against.

### Clarifying Questions

The goal is to surface ambiguity early and get to a concrete spec. A reliable way to structure your questions is to cover four areas: what the core actions are, how errors should be handled, what the boundaries of the system are, and whether we need to plan for future extensions.

> **You:** "What payment methods are supported?"
>
> **Interviewer:** "Cash only — coins and bills. No cards."

Good. Now figure out how products are organized.

> **You:** "How are products organized inside the machine?"
>
> **Interviewer:** "Slots. Each slot holds one type of product with a quantity and a price."

> **You:** "Can the machine give change?"
>
> **Interviewer:** "Yes. If the customer overpays, the machine returns exact change in coins/bills."

Now think about what happens when things go wrong.

> **You:** "What if a product is sold out?"
>
> **Interviewer:** "Reject the selection. The customer's money stays inserted — they can pick something else or cancel for a refund."

> **You:** "What if the machine can't make exact change?"
>
> **Interviewer:** "Refund all inserted money and don't dispense."

Finally, check for scope and admin operations.

> **You:** "Do we need admin operations like restocking?"
>
> **Interviewer:** "Yes, basic restock. But keep it simple."

> **You:** "Do we need to track sales or analytics?"
>
> **Interviewer:** "No, out of scope."

### Final Requirements

```
Requirements:
1. Machine holds N slots, each with a product type, quantity, and price (in cents)
2. Customer inserts money (coins/bills of known denominations)
3. Customer selects a product slot
4. Machine verifies sufficient balance and stock
5. Machine dispenses the product and returns exact change
6. If exact change cannot be made, refund all inserted money
7. Admin can restock slots

Out of scope:
- Card payments
- Sales analytics
- UI / display logic
- Multiple machines
```

---

## Core Entities and Relationships

Start by asking: what are the main "things" in this problem? Look for nouns in your requirements. In a Vending Machine, a few jump out: the machine itself, the slots holding products, the cash register managing money, and the state controlling which operations are valid at each stage.

| Entity | Responsibility |
|--------|---------------|
| **VendingMachine** | The state context. Holds slots and the cash register. Delegates every user action to the current VendingState. External code interacts only through this class. |
| **Slot** | A physical slot holding one product type. Tracks the product, quantity, and price. Handles dispense and restock at the slot level. |
| **CashRegister** | Manages inserted money and available denominations for change. Handles the greedy change-making algorithm. Separated from the machine's state logic to keep each class cohesive. |
| **VendingState** | State pattern interface. Each concrete state enforces which operations are valid — prevents invalid transitions (e.g., dispensing before payment) by design rather than by if-else chains. |

---

## Class Design

Now that we've identified the core entities, the next step is defining their interfaces. The State pattern is the key architectural choice here — it keeps VendingMachine clean and makes each state's rules explicit.

### VendingMachine

The VendingMachine is the State context. It holds the data (slots, cash register) but delegates all behaviour to the current state.

| Requirement | What VendingMachine must track |
|-------------|-------------------------------|
| "Machine holds N slots" | A map of slotId → Slot |
| "Customer inserts money" | A CashRegister |
| "Machine state changes as customer acts" | The current VendingState |

```
class VendingMachine:
    - slots: Map<String, Slot>
    - cashRegister: CashRegister
    - state: VendingState

    + VendingMachine()
    + addSlot(slot) → void
    + insertMoney(denomination) → void
    + selectProduct(slotId) → void
    + cancel() → void
    + restock(slotId, quantity) → void
```

### Slot

| Requirement | What Slot must track |
|-------------|---------------------|
| "Each slot has a product type, quantity, and price" | product, quantity, priceInCents |
| "Sold out rejection" | Stock check |

```
class Slot:
    - slotId: String
    - product: Product
    - quantity: int
    - priceInCents: int

    + hasStock() → boolean
    + dispense() → void
    + restock(amount) → void
```

### CashRegister

| Requirement | What CashRegister must track |
|-------------|------------------------------|
| "Customer inserts coins/bills" | Inserted amount, denomination counts |
| "Returns exact change" | Available denominations for change-making |
| "Refund if can't make change" | Ability to reverse all insertions |

```
class CashRegister:
    - register: Map<Denomination, Integer>
    - insertedCents: int

    + insert(denomination) → void
    + getInsertedCents() → int
    + processPayment(priceInCents) → Map<Denomination, Integer>?
    + refundAll() → Map<Denomination, Integer>?
```

### VendingState (State Pattern)

Three states: Idle (waiting for money), HasMoney (money inserted, awaiting selection), Dispensing (internal — processing).

```
interface VendingState:
    + insertMoney(machine, denomination) → void
    + selectProduct(machine, slotId) → void
    + cancel(machine) → void

class IdleState implements VendingState
class HasMoneyState implements VendingState
```

### Final Class Design

```
class VendingMachine:
    - slots: Map<String, Slot>
    - cashRegister: CashRegister
    - state: VendingState

    + VendingMachine()
    + addSlot(slot) → void
    + insertMoney(denomination) → void
    + selectProduct(slotId) → void
    + cancel() → void
    + restock(slotId, quantity) → void

class Slot:
    - slotId: String
    - product: Product
    - quantity: int
    - priceInCents: int

    + hasStock() → boolean
    + dispense() → void
    + restock(amount) → void

class Product:
    - productId: String
    - name: String

class CashRegister:
    - register: Map<Denomination, Integer>
    - insertedCents: int

    + insert(denomination) → void
    + getInsertedCents() → int
    + processPayment(priceInCents) → Map<Denomination, Integer>?
    + refundAll() → Map<Denomination, Integer>?

interface VendingState:
    + insertMoney(machine, denomination) → void
    + selectProduct(machine, slotId) → void
    + cancel(machine) → void

class IdleState implements VendingState
class HasMoneyState implements VendingState

enum Denomination:
    PENNY(1), NICKEL(5), DIME(10), QUARTER(25), DOLLAR(100), FIVE(500)
```

---

## Implementation

For each method, follow a consistent pattern: define the core logic (happy path), then consider edge cases.

### CashRegister.processPayment

This is the most interesting method — it implements greedy change-making.

**Core logic:**
1. Calculate change due = insertedCents - priceInCents
2. If change < 0, insufficient funds (handled by caller)
3. Use greedy algorithm: iterate denominations largest-first, use as many of each as available and needed
4. If exact change made, deduct from register, reset insertedCents
5. If not, refund everything

**Edge cases:**
- Change due is 0 → return empty map (exact payment)
- Can't make exact change → refund all

```
processPayment(priceInCents)
    changeDue = insertedCents - priceInCents
    if changeDue < 0
        return null

    change = makeChange(changeDue)
    if change is null
        refundAll()
        return null

    deduct change denominations from register
    insertedCents = 0
    return change

makeChange(amount)
    change = empty map
    for each denomination from largest to smallest:
        available = register[denomination]
        needed = amount / denomination.cents
        use = min(available, needed)
        if use > 0:
            change[denomination] = use
            amount -= use * denomination.cents
    return amount == 0 ? change : null
```

### HasMoneyState.selectProduct

**Core logic:**
1. Look up the slot
2. Check stock
3. Check sufficient funds
4. Process payment (which handles change-making)
5. Dispense product
6. Transition to IdleState

**Edge cases:**
- Invalid slot → reject
- Out of stock → reject (money stays, user can pick another)
- Insufficient funds → reject with message
- Can't make change → refund, transition to Idle

```
selectProduct(machine, slotId)
    slot = machine.getSlot(slotId)
    if slot is null → "Invalid slot"
    if !slot.hasStock() → "Out of stock"
    if machine.cashRegister.insertedCents < slot.priceInCents → "Insufficient funds"

    change = machine.cashRegister.processPayment(slot.priceInCents)
    if change is null
        "Cannot make change. Refunding."
        machine.setState(IdleState)
        return

    slot.dispense()
    print "Dispensed: " + slot.product.name
    print "Change: " + change
    machine.setState(IdleState)
```

### Complete Code Implementation

```java
public enum Denomination {
    PENNY(1), NICKEL(5), DIME(10), QUARTER(25), DOLLAR(100), FIVE(500);

    private final int cents;
    Denomination(int cents) { this.cents = cents; }
    public int getCents() { return cents; }
}
```

```java
public class Product {
    private final String productId;
    private final String name;

    public Product(String productId, String name) {
        this.productId = productId;
        this.name = name;
    }

    public String getProductId() { return productId; }
    public String getName()      { return name; }
}
```

```java
public class Slot {
    private final String slotId;
    private Product product;
    private int quantity;
    private int priceInCents;

    public Slot(String slotId, Product product, int quantity, int priceInCents) {
        this.slotId = slotId;
        this.product = product;
        this.quantity = quantity;
        this.priceInCents = priceInCents;
    }

    public boolean hasStock()    { return quantity > 0; }
    public Product getProduct()  { return product; }
    public int getPriceInCents() { return priceInCents; }
    public String getSlotId()    { return slotId; }

    void dispense()              { quantity--; }
    void restock(int amount)     { quantity += amount; }
}
```

```java
import java.util.EnumMap;
import java.util.Map;

public class CashRegister {
    private final Map<Denomination, Integer> register = new EnumMap<>(Denomination.class);
    private int insertedCents;

    public CashRegister() {
        for (Denomination d : Denomination.values()) {
            register.put(d, 10);
        }
        insertedCents = 0;
    }

    public void insert(Denomination denomination) {
        insertedCents += denomination.getCents();
        register.merge(denomination, 1, Integer::sum);
    }

    public int getInsertedCents() { return insertedCents; }

    public Map<Denomination, Integer> processPayment(int priceInCents) {
        int changeDue = insertedCents - priceInCents;
        if (changeDue < 0) return null;

        Map<Denomination, Integer> change = makeChange(changeDue);
        if (change == null) {
            refundAll();
            return null;
        }

        change.forEach((d, qty) -> register.merge(d, -qty, Integer::sum));
        insertedCents = 0;
        return change;
    }

    private Map<Denomination, Integer> makeChange(int amount) {
        Map<Denomination, Integer> change = new EnumMap<>(Denomination.class);
        Denomination[] sorted = {
            Denomination.FIVE, Denomination.DOLLAR,
            Denomination.QUARTER, Denomination.DIME,
            Denomination.NICKEL, Denomination.PENNY
        };
        for (Denomination d : sorted) {
            int available = register.get(d);
            int needed = amount / d.getCents();
            int use = Math.min(available, needed);
            if (use > 0) {
                change.put(d, use);
                amount -= use * d.getCents();
            }
        }
        return amount == 0 ? change : null;
    }

    public Map<Denomination, Integer> refundAll() {
        Map<Denomination, Integer> refund = makeChange(insertedCents);
        if (refund != null) {
            refund.forEach((d, qty) -> register.merge(d, -qty, Integer::sum));
        }
        insertedCents = 0;
        return refund;
    }
}
```

```java
public interface VendingState {
    void insertMoney(VendingMachine machine, Denomination denomination);
    void selectProduct(VendingMachine machine, String slotId);
    void cancel(VendingMachine machine);
}
```

```java
public class IdleState implements VendingState {
    @Override
    public void insertMoney(VendingMachine machine, Denomination denomination) {
        machine.getCashRegister().insert(denomination);
        System.out.printf("Inserted %s. Total: %d¢%n",
            denomination, machine.getCashRegister().getInsertedCents());
        machine.setState(new HasMoneyState());
    }

    @Override
    public void selectProduct(VendingMachine machine, String slotId) {
        System.out.println("Please insert money first.");
    }

    @Override
    public void cancel(VendingMachine machine) {
        System.out.println("Nothing to cancel.");
    }
}
```

```java
import java.util.Map;

public class HasMoneyState implements VendingState {
    @Override
    public void insertMoney(VendingMachine machine, Denomination denomination) {
        machine.getCashRegister().insert(denomination);
        System.out.printf("Inserted %s. Total: %d¢%n",
            denomination, machine.getCashRegister().getInsertedCents());
    }

    @Override
    public void selectProduct(VendingMachine machine, String slotId) {
        Slot slot = machine.getSlot(slotId);
        if (slot == null) {
            System.out.println("Invalid slot.");
            return;
        }
        if (!slot.hasStock()) {
            System.out.println("Out of stock. Select another product or cancel.");
            return;
        }
        if (machine.getCashRegister().getInsertedCents() < slot.getPriceInCents()) {
            System.out.printf("Insufficient funds. Need %d¢ more.%n",
                slot.getPriceInCents() - machine.getCashRegister().getInsertedCents());
            return;
        }

        Map<Denomination, Integer> change =
            machine.getCashRegister().processPayment(slot.getPriceInCents());

        if (change == null) {
            System.out.println("Cannot make exact change. Refunding.");
            machine.setState(new IdleState());
            return;
        }

        slot.dispense();
        System.out.println("Dispensed: " + slot.getProduct().getName());
        if (!change.isEmpty()) {
            System.out.println("Change: " + change);
        }
        machine.setState(new IdleState());
    }

    @Override
    public void cancel(VendingMachine machine) {
        Map<Denomination, Integer> refund = machine.getCashRegister().refundAll();
        System.out.println("Cancelled. Refund: " + refund);
        machine.setState(new IdleState());
    }
}
```

```java
import java.util.HashMap;
import java.util.Map;

public class VendingMachine {
    private final Map<String, Slot> slots;
    private final CashRegister cashRegister;
    private VendingState state;

    public VendingMachine() {
        slots = new HashMap<>();
        cashRegister = new CashRegister();
        state = new IdleState();
    }

    public void addSlot(Slot slot) { slots.put(slot.getSlotId(), slot); }

    public void insertMoney(Denomination denomination) {
        state.insertMoney(this, denomination);
    }

    public void selectProduct(String slotId) {
        state.selectProduct(this, slotId);
    }

    public void cancel() { state.cancel(this); }

    public Slot getSlot(String slotId)        { return slots.get(slotId); }
    public CashRegister getCashRegister()     { return cashRegister; }
    void setState(VendingState state)         { this.state = state; }

    public void restock(String slotId, int quantity) {
        Slot slot = slots.get(slotId);
        if (slot != null) slot.restock(quantity);
    }
}
```

### Verification

```
Setup: Machine with two slots:
  A1 → Coca-Cola, qty=5, price=125¢
  A2 → Lay's Chips, qty=3, price=150¢

Step 1: insertMoney(DOLLAR)
  State: Idle → insert 100¢ → print "Inserted DOLLAR. Total: 100¢"
  Transition → HasMoneyState

Step 2: insertMoney(QUARTER)
  State: HasMoney → insert 25¢ → print "Inserted QUARTER. Total: 125¢"
  Stay in HasMoneyState

Step 3: selectProduct("A1")
  Slot A1: hasStock? Yes. Price 125¢, inserted 125¢ → sufficient
  processPayment(125): changeDue = 0 → return empty map
  dispense() → qty 5→4
  print "Dispensed: Coca-Cola"
  Transition → IdleState

Step 4: insertMoney(QUARTER)
  State: Idle → insert 25¢ → HasMoneyState

Step 5: selectProduct("A2")
  Slot A2: price 150¢, inserted 25¢ → "Insufficient funds. Need 125¢ more."
  Stay in HasMoneyState

Step 6: cancel()
  refundAll() → makeChange(25) → {QUARTER=1}
  print "Cancelled. Refund: {QUARTER=1}"
  Transition → IdleState
```

This verifies the full State pattern lifecycle: Idle → HasMoney → dispense → Idle, insufficient funds rejection, and cancel/refund.

---

## Extensibility

### 1. "How would you add card payment?"

> "I'd introduce a `PaymentMethod` interface with `pay(amountInCents)` and `getAvailableBalance()` methods. CashRegister becomes one implementation; a new `CardPayment` class implements the same interface by delegating to an external payment gateway. VendingMachine would accept a PaymentMethod rather than being hardcoded to CashRegister. The State classes don't change — they just call the payment interface."

```
interface PaymentMethod:
    + pay(amountInCents) → boolean
    + getAvailableBalance() → int
    + refund() → void

class CashPayment implements PaymentMethod
class CardPayment implements PaymentMethod
```

### 2. "What if slots need temperature control (refrigerated vs ambient)?"

> "I'd add a `SlotType` enum (REFRIGERATED, AMBIENT) to Slot. A refrigerated slot would track a `currentTemperature` and have an `isOperational()` check. The selection logic in HasMoneyState would verify `slot.isOperational()` before dispensing. This keeps temperature concern localized to Slot without touching the State pattern."

```
enum SlotType:
    REFRIGERATED, AMBIENT

class Slot:
    - type: SlotType
    - currentTemperature: double

    + isOperational() → boolean
        if type == AMBIENT: return true
        return currentTemperature < 8.0
```

### 3. "How would you track sales analytics?"

> "I'd add an observer. After each successful dispense, VendingMachine notifies a `SalesListener` with a `SalesRecord` containing the product, price, and timestamp. The listener can log to a file, send to a service, etc. The core vending logic doesn't change. This follows the Observer pattern so analytics is decoupled from the purchase flow."

```
class SalesRecord:
    - productId: String
    - priceInCents: int
    - soldAt: LocalDateTime

interface SalesListener:
    + onSale(record: SalesRecord)

// In VendingMachine after dispense:
salesListeners.forEach(l -> l.onSale(new SalesRecord(...)))
```

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can break the vending machine into logical parts: something for the machine, something for slots, something for money. Your code should handle a basic purchase flow — insert money, pick a product, get it dispensed. Change-making might be simplified (just returning the difference as a single amount). State management with if-else is acceptable. If your machine correctly dispenses a product and rejects out-of-stock or insufficient-funds cases, you're doing well.

### Mid-level

For mid-level candidates, I expect the State pattern or at least a clear state machine. CashRegister should be its own class with proper denomination-based change-making. EnumMap is a nice touch for denomination tracking. You should handle all edge cases: exact change failure, out of stock, cancel/refund. I'd expect you to explain why the State pattern is better than a big switch-case and to discuss at least one extensibility scenario.

### Senior

Senior candidates should produce a crisp State pattern implementation with clear justification for each class boundary. You'd explain why CashRegister is separate, why greedy change-making works for standard US denominations (and when it wouldn't), and proactively discuss thread safety. Extensibility discussions should cover the payment strategy abstraction with real tradeoffs — card payment needs async handling and error recovery that cash doesn't. You might also discuss making the denomination set configurable for different currencies.