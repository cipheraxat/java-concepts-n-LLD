# ATM System

**Difficulty:** Medium | **Companies:** Amazon, Goldman Sachs, Oracle

---

## Requirements

> "Design an ATM system that supports card authentication, balance inquiry, cash withdrawal, and deposit."

### Clarifying Questions

> **You:** "How many types of transactions do we support?"
>
> **Interviewer:** "Balance inquiry, cash withdrawal, and cash deposit."

> **You:** "Should the ATM dispense optimal denominations?"
>
> **Interviewer:** "Yes — use a greedy approach with the largest denominations first."

> **You:** "Do we use a State pattern for the ATM flow?"
>
> **Interviewer:** "Yes — the ATM transitions through Idle → CardInserted → Authenticated → Transaction states."

> **You:** "Concurrency — can multiple threads access the same account?"
>
> **Interviewer:** "Yes. Use a ReentrantLock on BankAccount for atomic balance updates."

### Final Requirements

```
Requirements:
1. State pattern: ATM transitions through Idle, CardInserted, Authenticated
2. Card insertion and PIN authentication
3. Balance inquiry
4. Cash withdrawal with denomination optimization (greedy)
5. Cash deposit
6. Lock on BankAccount for concurrency
7. CashDispenser tracks available denominations

Out of scope:
- Card retention / blocked cards
- Multi-currency
- Receipt printing
- Network communication with bank server
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **ATM** | Context class. Holds current state, card slot, cash dispenser. Delegates actions to current state. |
| **ATMState** | Interface for state behavior: insertCard, authenticate, selectTransaction, executeTransaction, ejectCard. |
| **IdleState** | Accepts card insertion. Transitions to CardInserted. |
| **CardInsertedState** | Accepts PIN. Validates via BankService. Transitions to Authenticated. |
| **AuthenticatedState** | Accepts transaction requests. Executes them. |
| **BankAccount** | Account with balance and ReentrantLock. Supports debit/credit. |
| **CashDispenser** | Tracks denomination counts. Dispenses via greedy algorithm. |
| **Card** | Holds card number and associated account ID. |
| **BankService** | Validates PIN. Retrieves BankAccount by account ID. |

---

## Class Design

### ATMState (State pattern interface)

```
interface ATMState:
    + insertCard(card) → void
    + authenticate(pin) → boolean
    + balanceInquiry() → double
    + withdraw(amount) → boolean
    + deposit(amount) → void
    + ejectCard() → void
```

### ATM (Context)

```
class ATM:
    - state: ATMState
    - currentCard: Card?
    - currentAccount: BankAccount?
    - cashDispenser: CashDispenser
    - bankService: BankService

    + setState(state) → void
    + insertCard(card) → void     // delegate to state
    + authenticate(pin) → boolean
    + balanceInquiry() → double
    + withdraw(amount) → boolean
    + deposit(amount) → void
    + ejectCard() → void
```

### CashDispenser

```
class CashDispenser:
    - denominations: TreeMap<Integer, Integer> (desc order) // value → count

    + dispense(amount) → Map<Integer, Integer>  // greedy
    + addCash(denomination, count) → void
    + totalCash() → int
```

### BankAccount

```
class BankAccount:
    - accountId: String
    - balance: double
    - lock: ReentrantLock

    + getBalance() → double
    + debit(amount) → boolean
    + credit(amount) → void
```

---

## Implementation

### CashDispenser.dispense (greedy denomination)

```
dispense(amount)
    result = {}
    remaining = amount
    for each denomination in descending order:
        if remaining <= 0 → break
        count = min(remaining / denomination, available[denomination])
        if count > 0:
            result[denomination] = count
            remaining -= denomination * count
    if remaining > 0 → throw "Cannot dispense exact amount"
    // commit: deduct from available
    for each (denom, count) in result:
        available[denom] -= count
    return result
```

### ATM state transitions

```
IdleState.insertCard(card):
    atm.setCurrentCard(card)
    atm.setState(new CardInsertedState(atm))

CardInsertedState.authenticate(pin):
    account = bankService.authenticate(card, pin)
    if account is null → return false (3 attempts max)
    atm.setCurrentAccount(account)
    atm.setState(new AuthenticatedState(atm))
    return true

AuthenticatedState.withdraw(amount):
    account.lock()
    try:
        if account.getBalance() < amount → return false
        bills = cashDispenser.dispense(amount) // may throw
        account.debit(amount)
        return true
    finally:
        account.unlock()

AuthenticatedState.ejectCard():
    atm.setCurrentCard(null)
    atm.setCurrentAccount(null)
    atm.setState(new IdleState(atm))
```

### Complete Code Implementation

```java
public class Card {
    private final String cardNumber;
    private final String accountId;

    public Card(String cardNumber, String accountId) {
        this.cardNumber = cardNumber;
        this.accountId = accountId;
    }

    public String getCardNumber() { return cardNumber; }
    public String getAccountId() { return accountId; }
}
```

```java
import java.util.concurrent.locks.ReentrantLock;

public class BankAccount {
    private final String accountId;
    private double balance;
    private final ReentrantLock lock = new ReentrantLock();

    public BankAccount(String accountId, double balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public String getAccountId() { return accountId; }

    public double getBalance() {
        lock.lock();
        try { return balance; }
        finally { lock.unlock(); }
    }

    public boolean debit(double amount) {
        lock.lock();
        try {
            if (balance < amount) return false;
            balance -= amount;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void credit(double amount) {
        lock.lock();
        try { balance += amount; }
        finally { lock.unlock(); }
    }

    public ReentrantLock getLock() { return lock; }
}
```

```java
import java.util.*;

public class CashDispenser {
    private final TreeMap<Integer, Integer> denominations =
        new TreeMap<>(Collections.reverseOrder());

    public void addCash(int denomination, int count) {
        denominations.merge(denomination, count, Integer::sum);
    }

    public Map<Integer, Integer> dispense(int amount) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        int remaining = amount;

        for (Map.Entry<Integer, Integer> entry : denominations.entrySet()) {
            if (remaining <= 0) break;
            int denom = entry.getKey();
            int available = entry.getValue();
            int count = Math.min(remaining / denom, available);
            if (count > 0) {
                result.put(denom, count);
                remaining -= denom * count;
            }
        }

        if (remaining > 0)
            throw new IllegalStateException("Cannot dispense exact amount: " + amount);

        // Commit
        for (Map.Entry<Integer, Integer> e : result.entrySet()) {
            denominations.merge(e.getKey(), -e.getValue(), Integer::sum);
        }
        return result;
    }

    public int totalCash() {
        return denominations.entrySet().stream()
            .mapToInt(e -> e.getKey() * e.getValue()).sum();
    }
}
```

```java
public interface ATMState {
    void insertCard(Card card);
    boolean authenticate(String pin);
    double balanceInquiry();
    boolean withdraw(int amount);
    void deposit(int amount);
    void ejectCard();
}
```

```java
public class IdleState implements ATMState {
    private final ATM atm;

    public IdleState(ATM atm) { this.atm = atm; }

    @Override
    public void insertCard(Card card) {
        atm.setCurrentCard(card);
        atm.setState(new CardInsertedState(atm));
    }

    @Override public boolean authenticate(String pin)
        { throw new IllegalStateException("Insert card first"); }
    @Override public double balanceInquiry()
        { throw new IllegalStateException("Insert card first"); }
    @Override public boolean withdraw(int amount)
        { throw new IllegalStateException("Insert card first"); }
    @Override public void deposit(int amount)
        { throw new IllegalStateException("Insert card first"); }
    @Override public void ejectCard()
        { throw new IllegalStateException("No card inserted"); }
}
```

```java
public class CardInsertedState implements ATMState {
    private final ATM atm;
    private int attempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    public CardInsertedState(ATM atm) { this.atm = atm; }

    @Override
    public boolean authenticate(String pin) {
        BankAccount account = atm.getBankService()
            .authenticate(atm.getCurrentCard(), pin);
        if (account != null) {
            atm.setCurrentAccount(account);
            atm.setState(new AuthenticatedState(atm));
            return true;
        }
        attempts++;
        if (attempts >= MAX_ATTEMPTS) {
            atm.setCurrentCard(null);
            atm.setState(new IdleState(atm));
        }
        return false;
    }

    @Override public void insertCard(Card card)
        { throw new IllegalStateException("Card already inserted"); }
    @Override public double balanceInquiry()
        { throw new IllegalStateException("Authenticate first"); }
    @Override public boolean withdraw(int amount)
        { throw new IllegalStateException("Authenticate first"); }
    @Override public void deposit(int amount)
        { throw new IllegalStateException("Authenticate first"); }

    @Override
    public void ejectCard() {
        atm.setCurrentCard(null);
        atm.setState(new IdleState(atm));
    }
}
```

```java
public class AuthenticatedState implements ATMState {
    private final ATM atm;

    public AuthenticatedState(ATM atm) { this.atm = atm; }

    @Override
    public double balanceInquiry() {
        return atm.getCurrentAccount().getBalance();
    }

    @Override
    public boolean withdraw(int amount) {
        BankAccount account = atm.getCurrentAccount();
        account.getLock().lock();
        try {
            if (account.getBalance() < amount) return false;
            atm.getCashDispenser().dispense(amount);
            account.debit(amount);
            return true;
        } catch (IllegalStateException e) {
            return false; // cannot dispense exact amount
        } finally {
            account.getLock().unlock();
        }
    }

    @Override
    public void deposit(int amount) {
        atm.getCurrentAccount().credit(amount);
    }

    @Override
    public void ejectCard() {
        atm.setCurrentCard(null);
        atm.setCurrentAccount(null);
        atm.setState(new IdleState(atm));
    }

    @Override public void insertCard(Card card)
        { throw new IllegalStateException("Card already inserted"); }
    @Override public boolean authenticate(String pin)
        { throw new IllegalStateException("Already authenticated"); }
}
```

```java
import java.util.HashMap;
import java.util.Map;

public class BankService {
    private final Map<String, BankAccount> accounts = new HashMap<>();
    private final Map<String, String> pins = new HashMap<>(); // cardNumber → pin

    public void addAccount(BankAccount account) {
        accounts.put(account.getAccountId(), account);
    }

    public void registerCard(String cardNumber, String pin, String accountId) {
        pins.put(cardNumber, pin);
    }

    public BankAccount authenticate(Card card, String pin) {
        String expected = pins.get(card.getCardNumber());
        if (expected != null && expected.equals(pin))
            return accounts.get(card.getAccountId());
        return null;
    }
}
```

```java
public class ATM {
    private ATMState state;
    private Card currentCard;
    private BankAccount currentAccount;
    private final CashDispenser cashDispenser;
    private final BankService bankService;

    public ATM(BankService bankService) {
        this.bankService = bankService;
        this.cashDispenser = new CashDispenser();
        this.state = new IdleState(this);
    }

    public void insertCard(Card card) { state.insertCard(card); }
    public boolean authenticate(String pin) { return state.authenticate(pin); }
    public double balanceInquiry() { return state.balanceInquiry(); }
    public boolean withdraw(int amount) { return state.withdraw(amount); }
    public void deposit(int amount) { state.deposit(amount); }
    public void ejectCard() { state.ejectCard(); }

    public void setState(ATMState state) { this.state = state; }
    public Card getCurrentCard() { return currentCard; }
    public void setCurrentCard(Card card) { this.currentCard = card; }
    public BankAccount getCurrentAccount() { return currentAccount; }
    public void setCurrentAccount(BankAccount account) { this.currentAccount = account; }
    public CashDispenser getCashDispenser() { return cashDispenser; }
    public BankService getBankService() { return bankService; }
}
```

### Verification

```
Setup: BankAccount("ACC1", 5000.0). Card("CARD1", "ACC1"). PIN = "1234".
       CashDispenser: {100→50, 500→20, 2000→5}.

Step 1: atm.insertCard(card1)
  State = Idle → insertCard → setCurrentCard(card1), setState(CardInserted) ✓

Step 2: atm.authenticate("0000") → wrong PIN
  bankService.authenticate → null. attempts=1. return false ✓

Step 3: atm.authenticate("1234") → correct PIN
  bankService.authenticate → ACC1. setCurrentAccount(ACC1).
  setState(Authenticated) ✓

Step 4: atm.balanceInquiry() → 5000.0 ✓

Step 5: atm.withdraw(3600)
  balance=5000 ≥ 3600 ✓
  dispense(3600): 2000×1=2000, remaining=1600.
    500×3=1500, remaining=100. 100×1=100, remaining=0.
  Result: {2000:1, 500:3, 100:1}. account.debit(3600).
  Balance = 1400.0 ✓

Step 6: atm.withdraw(2000)
  balance=1400 < 2000 → return false ✓

Step 7: atm.deposit(600)
  account.credit(600). Balance = 2000.0 ✓

Step 8: atm.ejectCard()
  currentCard=null, currentAccount=null, setState(Idle) ✓

Step 9: atm.authenticate("1234") → throws "Insert card first" ✓
```

---

## Extensibility

### 1. "How would you add transfer between accounts?"

> "I'd add a `transfer(fromAccountId, toAccountId, amount)` transaction type. Both accounts are locked in a consistent order (by account ID) to prevent deadlocks. The debit from the source and credit to the destination happen within the same locked scope. This becomes a new option in the AuthenticatedState."

### 2. "How would you add receipt printing?"

> "I'd introduce a `ReceiptPrinter` interface with a `print(TransactionRecord)` method. After each successful transaction, the ATM creates a TransactionRecord (type, amount, balance, timestamp) and passes it to the printer. This follows the Open/Closed principle — the core logic doesn't change."

### 3. "How would you support multi-currency?"

> "Each denomination tracks a currency code. The CashDispenser maintains a Map<Currency, TreeMap<Integer, Integer>>. Withdrawal requests specify a currency. Exchange rates could be provided via a CurrencyExchangeService, but the dispense logic stays the same — just scoped to the requested currency's denominations."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should implement the State pattern correctly with at least three states (Idle, CardInserted, Authenticated). Basic card insertion, PIN validation, balance inquiry, and simple withdrawal are the key features. The state transitions should be clean and each state should reject invalid operations.

### Mid-level

Mid-level candidates should implement the CashDispenser with greedy denomination optimization, use ReentrantLock on BankAccount for thread-safe balance operations, and handle edge cases like insufficient balance, insufficient cash in dispenser, and max PIN attempts with card ejection. The State pattern should be fully fleshed out.

### Senior

Senior candidates would discuss deadlock prevention when transferring between accounts (ordered locking), the limitations of greedy denomination dispensing vs. dynamic programming for certain denomination sets, and how to integrate with a remote bank server asynchronously. Idempotency of transactions and audit logging should also be addressed.