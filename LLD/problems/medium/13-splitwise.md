# Splitwise

**Difficulty:** Medium | **Companies:** Google, Amazon, Uber

---

## Requirements

> "Design the backend for a Splitwise-like expense sharing application. Users can create groups, add expenses with different split types, track balances, and simplify debts."

### Clarifying Questions

> **You:** "What split types do we need?"
>
> **Interviewer:** "Equal split, exact amounts, and percentage-based."

> **You:** "Should we support group-level debt simplification?"
>
> **Interviewer:** "Yes. Minimize the number of transactions needed to settle all debts in a group."

> **You:** "Do we need a payment/settlement feature?"
>
> **Interviewer:** "Yes — users can record that they've paid someone, which reduces the balance."

> **You:** "Multi-currency support?"
>
> **Interviewer:** "No, single currency is fine."

### Final Requirements

```
Requirements:
1. Users create groups and add members
2. Add expenses with split types: EQUAL, EXACT, PERCENT
3. Track pairwise balances within a group
4. Simplify debts to minimize transactions (greedy algorithm)
5. Record settlements between users

Out of scope:
- Multi-currency
- Payment processing
- Expense categories / tags
- Recurring expenses
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **SplitwiseService** | Facade. Manages users, groups, expenses. Delegates split logic to strategy. |
| **User** | Identity with a name. Referenced by expenses and groups. |
| **Group** | Collection of users. Holds expenses and a BalanceSheet. |
| **Expense** | An amount paid by a user, split among participants. Uses a SplitStrategy. |
| **Split** | Abstract base: each participant's share. Subclasses: EqualSplit, ExactSplit, PercentSplit. |
| **BalanceSheet** | Tracks net balances between user pairs in a group. Provides debt simplification. |

---

## Class Design

### SplitwiseService

```
class SplitwiseService:
    - users: Map<String, User>
    - groups: Map<String, Group>

    + addExpense(groupId, paidBy, amount, splits) → Expense
    + recordSettlement(groupId, fromUser, toUser, amount) → void
    + getBalances(groupId) → Map<(User,User), Double>
    + simplifyDebts(groupId) → List<Transaction>
```

### Expense & Split Hierarchy

| Requirement | What needs tracking |
|-------------|---------------------|
| "EQUAL split" | Divide total evenly across N participants |
| "EXACT split" | Each participant has a specified amount (must sum to total) |
| "PERCENT split" | Each participant has a percentage (must sum to 100%) |

```
class Expense:
    - expenseId: String
    - paidBy: User
    - amount: double
    - splits: List<Split>
    - description: String

abstract class Split:
    - user: User
    + getAmount(totalAmount, participantCount) → double

class EqualSplit extends Split:
    + getAmount(total, count) → total / count

class ExactSplit extends Split:
    - exactAmount: double
    + getAmount(total, count) → exactAmount

class PercentSplit extends Split:
    - percent: double
    + getAmount(total, count) → total × percent / 100
```

### BalanceSheet

```
class BalanceSheet:
    - balances: Map<String, Map<String, Double>>
      // balances[A][B] > 0 means B owes A

    + updateBalance(creditor, debtor, amount) → void
    + getBalance(userA, userB) → double
    + simplifyDebts() → List<Transaction>
```

### Final Class Design

```
class SplitwiseService: facade — users, groups
class User: userId, name
class Group: groupId, name, members, expenses, balanceSheet
class Expense: paidBy, amount, splits
abstract class Split → EqualSplit, ExactSplit, PercentSplit
class BalanceSheet: pairwise balance tracking + simplification
class Transaction: from, to, amount (result of simplification)
```

---

## Implementation

### SplitwiseService.addExpense

**Core logic:**
1. Validate splits (exact amounts must sum to total, percents must sum to 100%)
2. For each split, calculate the participant's share
3. Update the balance sheet: payer is owed by each participant

```
addExpense(groupId, paidBy, amount, splits)
    group = groups[groupId]
    validate(splits, amount)

    expense = new Expense(nextId(), paidBy, amount, splits)
    group.addExpense(expense)

    for each split in splits:
        if split.user == paidBy → continue
        share = split.getAmount(amount, splits.size)
        group.balanceSheet.updateBalance(paidBy, split.user, share)

    return expense
```

### BalanceSheet.updateBalance

**Core logic:** Net balances. If A owes B $10 and B owes A $3, store A→B = $7.

```
updateBalance(creditor, debtor, amount)
    // creditor is owed "amount" by debtor
    existing = balances[debtor][creditor]  // does debtor owe creditor?
    if existing >= 0:
        balances[debtor][creditor] = existing + amount
    // Simplify: if A owes B and B owes A, net them out
```

### BalanceSheet.simplifyDebts (Greedy with max-heap/min-heap)

**Algorithm:** Compute net balance per user. Positive = net creditor, negative = net debtor. Use two heaps: max-heap of creditors, min-heap of debtors. Match the largest creditor with the largest debtor, settle the minimum of the two amounts.

```
simplifyDebts()
    netBalances = compute net for each user
    creditors = maxHeap of (user, positiveAmount)
    debtors = maxHeap of (user, |negativeAmount|)

    transactions = []
    while creditors and debtors not empty:
        creditor = creditors.poll()
        debtor = debtors.poll()
        settled = min(creditor.amount, debtor.amount)
        transactions.add(Transaction(debtor.user, creditor.user, settled))

        if creditor.amount > settled:
            creditors.offer((creditor.user, creditor.amount - settled))
        if debtor.amount > settled:
            debtors.offer((debtor.user, debtor.amount - settled))

    return transactions
```

### Complete Code Implementation

```java
public class User {
    private final String userId;
    private final String name;

    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() { return userId; }
    public String getName()   { return name; }
}
```

```java
public abstract class Split {
    private final User user;

    protected Split(User user) { this.user = user; }

    public User getUser() { return user; }
    public abstract double getAmount(double totalAmount, int participantCount);
}

public class EqualSplit extends Split {
    public EqualSplit(User user) { super(user); }

    @Override
    public double getAmount(double totalAmount, int participantCount) {
        return totalAmount / participantCount;
    }
}

public class ExactSplit extends Split {
    private final double exactAmount;

    public ExactSplit(User user, double exactAmount) {
        super(user);
        this.exactAmount = exactAmount;
    }

    @Override
    public double getAmount(double totalAmount, int participantCount) {
        return exactAmount;
    }
}

public class PercentSplit extends Split {
    private final double percent;

    public PercentSplit(User user, double percent) {
        super(user);
        this.percent = percent;
    }

    @Override
    public double getAmount(double totalAmount, int participantCount) {
        return totalAmount * percent / 100.0;
    }
}
```

```java
import java.util.List;

public class Expense {
    private final String expenseId;
    private final User paidBy;
    private final double amount;
    private final List<Split> splits;
    private final String description;

    public Expense(String expenseId, User paidBy, double amount,
                   List<Split> splits, String description) {
        this.expenseId = expenseId;
        this.paidBy = paidBy;
        this.amount = amount;
        this.splits = splits;
        this.description = description;
    }

    public String getExpenseId() { return expenseId; }
    public User getPaidBy()      { return paidBy; }
    public double getAmount()    { return amount; }
    public List<Split> getSplits() { return splits; }
}
```

```java
public class Transaction {
    private final User from;
    private final User to;
    private final double amount;

    public Transaction(User from, User to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public User getFrom()     { return from; }
    public User getTo()       { return to; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return from.getName() + " pays " + to.getName() + " $" + String.format("%.2f", amount);
    }
}
```

```java
import java.util.*;

public class BalanceSheet {
    // balances[A][B] > 0 means A is owed by B (B owes A)
    private final Map<String, Map<String, Double>> balances = new HashMap<>();

    public void updateBalance(User creditor, User debtor, double amount) {
        balances.computeIfAbsent(creditor.getUserId(), k -> new HashMap<>())
            .merge(debtor.getUserId(), amount, Double::sum);
        balances.computeIfAbsent(debtor.getUserId(), k -> new HashMap<>())
            .merge(creditor.getUserId(), -amount, Double::sum);
    }

    public void recordSettlement(User from, User to, double amount) {
        updateBalance(to, from, -amount);
    }

    public double getBalance(User a, User b) {
        return balances.getOrDefault(a.getUserId(), Collections.emptyMap())
            .getOrDefault(b.getUserId(), 0.0);
    }

    public List<Transaction> simplifyDebts(Map<String, User> userLookup) {
        Map<String, Double> netBalances = new HashMap<>();
        for (var entry : balances.entrySet()) {
            String userId = entry.getKey();
            double net = entry.getValue().values().stream()
                .mapToDouble(Double::doubleValue).sum();
            if (Math.abs(net) > 0.01) {
                netBalances.put(userId, net);
            }
        }

        PriorityQueue<Map.Entry<String, Double>> creditors =
            new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<String, Double>> debtors =
            new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (var entry : netBalances.entrySet()) {
            if (entry.getValue() > 0.01) {
                creditors.offer(entry);
            } else if (entry.getValue() < -0.01) {
                debtors.offer(Map.entry(entry.getKey(), -entry.getValue()));
            }
        }

        List<Transaction> transactions = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var creditor = creditors.poll();
            var debtor = debtors.poll();
            double settled = Math.min(creditor.getValue(), debtor.getValue());
            transactions.add(new Transaction(
                userLookup.get(debtor.getKey()),
                userLookup.get(creditor.getKey()), settled));

            double creditorRemaining = creditor.getValue() - settled;
            double debtorRemaining = debtor.getValue() - settled;
            if (creditorRemaining > 0.01)
                creditors.offer(Map.entry(creditor.getKey(), creditorRemaining));
            if (debtorRemaining > 0.01)
                debtors.offer(Map.entry(debtor.getKey(), debtorRemaining));
        }

        return transactions;
    }
}
```

```java
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Group {
    private final String groupId;
    private final String name;
    private final Map<String, User> members = new LinkedHashMap<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final BalanceSheet balanceSheet = new BalanceSheet();

    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public void addMember(User user)       { members.put(user.getUserId(), user); }
    public void addExpense(Expense expense) { expenses.add(expense); }

    public String getGroupId()              { return groupId; }
    public Map<String, User> getMembers()   { return members; }
    public BalanceSheet getBalanceSheet()    { return balanceSheet; }
}
```

```java
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SplitwiseService {
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();
    private final AtomicLong expenseCounter = new AtomicLong(1);

    public void addUser(User user)   { users.put(user.getUserId(), user); }
    public void addGroup(Group group) { groups.put(group.getGroupId(), group); }

    public Expense addExpense(String groupId, User paidBy, double amount,
                              List<Split> splits, String description) {
        Group group = groups.get(groupId);
        validateSplits(splits, amount);

        Expense expense = new Expense(
            "E-" + expenseCounter.getAndIncrement(), paidBy, amount, splits, description);
        group.addExpense(expense);

        int count = splits.size();
        for (Split split : splits) {
            if (split.getUser().getUserId().equals(paidBy.getUserId())) continue;
            double share = split.getAmount(amount, count);
            group.getBalanceSheet().updateBalance(paidBy, split.getUser(), share);
        }

        return expense;
    }

    public void recordSettlement(String groupId, User from, User to, double amount) {
        Group group = groups.get(groupId);
        group.getBalanceSheet().recordSettlement(from, to, amount);
    }

    public List<Transaction> simplifyDebts(String groupId) {
        Group group = groups.get(groupId);
        return group.getBalanceSheet().simplifyDebts(group.getMembers());
    }

    private void validateSplits(List<Split> splits, double totalAmount) {
        if (splits.isEmpty()) throw new IllegalArgumentException("Splits cannot be empty");
        if (splits.get(0) instanceof ExactSplit) {
            double sum = splits.stream()
                .mapToDouble(s -> s.getAmount(totalAmount, splits.size())).sum();
            if (Math.abs(sum - totalAmount) > 0.01)
                throw new IllegalArgumentException("Exact splits must sum to total");
        } else if (splits.get(0) instanceof PercentSplit) {
            double sum = splits.stream()
                .mapToDouble(s -> ((PercentSplit) s).getAmount(totalAmount, splits.size()))
                .sum();
            if (Math.abs(sum - totalAmount) > 0.01)
                throw new IllegalArgumentException("Percent splits must sum to 100%");
        }
    }
}
```

### Verification

```
Setup: Group with Alice, Bob, Carol.

Step 1: Alice pays $90 dinner, split EQUAL among all 3.
  Each share = $90 / 3 = $30
  Bob owes Alice $30, Carol owes Alice $30.
  Balances: Alice→Bob: 30, Alice→Carol: 30

Step 2: Bob pays $60 taxi, split EQUAL among all 3.
  Each share = $60 / 3 = $20
  Alice owes Bob $20, Carol owes Bob $20.
  Balances: Alice→Bob: 30-20=10, Alice→Carol: 30, Bob→Carol: 20

Step 3: simplifyDebts()
  Net balances: Alice = +10+30-20 = +20 (creditor)
                Bob = -10+20 = +10 (creditor)
                Carol = -30-20 = -50 (debtor... wait, that's -30)

  Recalculate: Alice paid 90, owes 20 → net = +40? Let me use the balance map.
  Actually net from BalanceSheet:
    Alice: owed $30 by Bob reduced by $20 she owes Bob = net $10 from Bob, plus $30 from Carol = +$40
  Hmm, let me trace precisely:
    After step 1: balances[Alice][Bob]=30, balances[Alice][Carol]=30, balances[Bob][Alice]=-30, balances[Carol][Alice]=-30
    After step 2: balances[Bob][Alice]=-30+20=-10→ balances[Alice][Bob]..., balances[Bob][Carol]=20, balances[Carol][Bob]=-20

  Net per user:
    Alice: sum of balances[Alice][*] = 30 + 30 + (-20) = 40 → wait.

  Using updateBalance: step2 adds Bob→Alice: +20, Alice→Bob: -20
    balances[Alice]: {Bob: 30+(-20)=10, Carol: 30} → net = +40
    balances[Bob]: {Alice: -30+20=-10, Carol: 20} → net = +10
    balances[Carol]: {Alice: -30, Bob: -20} → net = -50

  Creditors: Alice(+40), Bob(+10). Debtors: Carol(50).
  Match Carol(50) with Alice(40) → Carol pays Alice $40. Carol remaining=10.
  Match Carol(10) with Bob(10) → Carol pays Bob $10.
  Result: 2 transactions instead of potentially 3. ✓
```

---

## Extensibility

### 1. "How would you handle expenses across multiple groups?"

> "I'd add a global BalanceSheet on SplitwiseService tracking net balances across all groups. When a user adds an expense to a group, both the group-level and global-level balance sheets are updated. SimplifyDebts can operate at either scope."

### 2. "How would you add expense splitting by shares (e.g., 2:3:5)?"

> "I'd add a `ShareSplit` subclass where each participant has an integer share weight. `getAmount` computes `totalAmount × (myShares / totalShares)`. It plugs directly into the existing Split hierarchy."

### 3. "How would you handle rounding errors in equal splits?"

> "For $100 split 3 ways, each share is $33.33, losing $0.01. I'd assign the remainder cent to the first participant. The split method returns exact cents: [3334, 3333, 3333] for $100.00 in cents. This ensures the total always sums correctly."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model User, Group, and Expense correctly. Equal splitting should work. Tracking who owes whom in a simple map is the main goal. The split hierarchy isn't required — a type enum with if/else is acceptable.

### Mid-level

Mid-level candidates should implement the Split hierarchy (Equal, Exact, Percent) using polymorphism. The BalanceSheet should correctly net out bidirectional debts. Validation of splits (exact amounts summing to total) is expected. You should be able to explain the debt simplification algorithm conceptually.

### Senior

Senior candidates would implement the greedy debt simplification with dual heaps. You'd discuss why this is optimal for minimizing transactions and the NP-hard nature of the general problem (minimum cash flow). Handling rounding errors, concurrent expense additions, and scaling to large groups should all be addressed.