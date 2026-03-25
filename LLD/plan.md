# Low-Level Design (LLD) — Complete Study Plan

> Based on the [HelloInterview](https://www.hellointerview.com/learn/low-level-design) format and delivery framework.

---

## Table of Contents

1. [Lessons](#lessons)
   - [Introduction to LLD](#1-introduction-to-lld)
   - [Delivery Framework](#2-delivery-framework)
   - [Design Principles](#3-design-principles)
   - [OOP Concepts](#4-oop-concepts)
   - [Design Patterns](#5-design-patterns)
   - [Concurrency](#6-concurrency)
2. [Problem Breakdowns](#problem-breakdowns)
   - [Easy](#easy)
   - [Medium](#medium)
   - [Hard](#hard)

---

# Lessons

---

## 1. Introduction to LLD

### What is LLD?

Low-Level Design (also called Object-Oriented Design or Machine Coding) tests your ability to translate a vague problem into clean, working, object-oriented code under time pressure (~45 min).

### Two Variants

| Variant | Description | Time | Deliverable |
|---------|-------------|------|-------------|
| **Whiteboard OOD** | Design classes, relationships, and key methods on a whiteboard | 45 min | Class diagram + pseudocode |
| **Machine Coding** | Write actual runnable code in an IDE | 60–90 min | Working code |

### Assessment Criteria

| Criterion | What interviewers look for |
|-----------|---------------------------|
| **Problem Exploration** | Do you ask clarifying questions? Do you scope the problem correctly? |
| **Data Modeling** | Are entities well-chosen? Are relationships clean? |
| **Class Design** | Is state minimal and behavior well-placed? SRP? |
| **Code Quality** | Clean, readable, idiomatic code? Meaningful names? |
| **Extensibility** | Can the design handle new requirements without rewrites? |

### Key Insight

> LLD is not about getting the "right" answer — it's about demonstrating clean thinking and structured communication. Multiple valid designs exist; what matters is your reasoning.

---

## 2. Delivery Framework

The **5-Step Delivery Framework** provides a structured approach to solve any LLD problem in ~45 minutes.

### Overview

| Step | Time | Goal |
|------|------|------|
| 1. Requirements | ~5 min | Clarify scope, pin down functional requirements |
| 2. Entities | ~3 min | Identify core entities and relationships |
| 3. Class Design | ~10–15 min | Define state (attributes) and behavior (methods) per class |
| 4. Implementation | ~10 min | Write pseudocode/code for core flows |
| 5. Extensibility | ~5 min | Show design handles follow-up modifications |

---

### Step 1: Requirements (~5 min)

**Goal:** Convert a vague prompt into a concrete spec.

**Process:**
1. Ask 3–5 clarifying questions to narrow scope
2. State your final requirements as a numbered list
3. Get interviewer agreement before proceeding

**Example (Tic-Tac-Toe):**
- "Is this standard 3×3 or configurable board size?" → Standard 3×3
- "Two human players, or is there an AI?" → Two human players
- "Any special rules beyond standard?" → No

**Final Requirements:**
1. Two players take turns placing X and O on a 3×3 grid
2. First player to get 3 in a row (horizontal, vertical, diagonal) wins
3. If all 9 cells are filled with no winner, it's a draw
4. Players cannot place on an already-occupied cell

**Tips:**
- Don't over-scope. Pick 3–5 core requirements.
- Avoid feature creep (no auth system, no database, no UI).
- Requirements drive everything else — get them right.

---

### Step 2: Entities and Relationships (~3 min)

**Goal:** Identify the core nouns (entities) and how they relate.

**Process:**
1. Extract nouns from your requirements → candidate entities
2. Filter: keep only entities with meaningful state or behavior
3. Define relationships (has-a, uses-a, is-a)

**Example (Tic-Tac-Toe):**
- **Game** — orchestrates the game loop
- **Board** — holds the grid state
- **Player** — represents X or O player

Relationships:
- Game *has-a* Board
- Game *has* two Players
- Board *has* cells (2D array)

**Tips:**
- Keep it simple. 3–6 entities is typical.
- Don't model everything — only what your requirements need.
- Use composition (has-a) over inheritance by default.

---

### Step 3: Class Design (~10–15 min)

**Goal:** Define state and behavior for each entity.

**Process — Top-Down Approach:**
1. **Derive state from requirements** — What data does each entity need?
2. **Derive behavior from requirements** — What actions must each entity support?
3. **Place behavior on the entity that owns the data** (Information Expert principle)

**Design Rules:**
- State should be **minimal** — don't store what you can compute
- Each class should have a **single responsibility** (SRP)
- **Hide internal state** — expose only necessary methods (Encapsulation)
- Use **enums** for fixed sets of values

**Example (Tic-Tac-Toe):**

```
class Player:
    name: String
    symbol: Symbol  // X or O

class Board:
    grid: Symbol[3][3]
    
    placePiece(row, col, symbol) → bool
    checkWinner() → Symbol?
    isFull() → bool

class Game:
    board: Board
    players: Player[2]
    currentPlayerIndex: int
    status: GameStatus  // IN_PROGRESS, WIN, DRAW
    
    makeMove(row, col) → MoveResult
    switchTurn()
    getStatus() → GameStatus
```

**Tips:**
- Start with the "God class" (Game/Controller) that orchestrates everything
- Push logic down to entities that own the relevant data
- Don't over-engineer: no interfaces unless you need polymorphism

---

### Step 4: Implementation (~10 min)

**Goal:** Write core flows in pseudocode or real code.

**Process:**
1. Implement the **happy path** first (main game loop, primary use case)
2. Then handle **edge cases** and validation
3. **Verify** by tracing through a sample scenario

**Implementation Order:**
1. Core flow (e.g., makeMove → place piece → check win → switch turn)
2. Validation (e.g., is cell occupied? is game over?)
3. Edge cases (e.g., draw condition)

**Verification Trace:**
Walk through a concrete scenario step-by-step:
```
Move 1: Player X at (0,0) → Board: [X, _, _; _, _, _; _, _, _] → No winner → Switch to O
Move 2: Player O at (1,1) → Board: [X, _, _; _, O, _; _, _, _] → No winner → Switch to X
...
```

**Tips:**
- Don't try to implement everything — focus on 1–2 core flows
- Code should compile/run mentally — no hand-waving
- Verification is critical — it proves your design works

---

### Step 5: Extensibility (~5 min)

**Goal:** Show your design accommodates new requirements without major rewrites.

**Common Follow-ups:**
- "What if we add a new type/variant?" (Strategy pattern, polymorphism)
- "What if we need to support undo?" (Command pattern, stack)
- "What if we need observers/notifications?" (Observer pattern)
- "What if we need to scale to multiple instances?" (Concurrency)

**Process:**
1. Listen to the "what if" question
2. Explain which class/interface you'd modify or extend
3. Show it's a **localized change** (OCP — Open/Closed Principle)
4. Briefly describe the implementation — no need to code it

**Example:**
> "What if we want to support a 5×5 board?"
>
> We'd parameterize the Board constructor: `Board(size, winLength)`. The `checkWinner()` method already works with generic row/col checks, so we just generalize the loop bounds. No other class needs to change.

---

## 3. Design Principles

### General Principles

| Principle | Description | Violation Sign |
|-----------|-------------|----------------|
| **KISS** | Keep It Simple, Stupid — prefer the simplest solution that works | Over-engineering, unnecessary abstractions |
| **DRY** | Don't Repeat Yourself — single source of truth for every piece of knowledge | Copy-pasted code, duplicated logic |
| **YAGNI** | You Aren't Gonna Need It — don't build for hypothetical future needs | Unused interfaces, speculative features |
| **Separation of Concerns** | Each module/class handles one concern | God classes, mixed responsibilities |
| **Law of Demeter** | Talk only to your immediate friends (don't chain: `a.getB().getC().doX()`) | Long method chains, tight coupling |

### SOLID Principles

#### S — Single Responsibility Principle (SRP)
> A class should have only one reason to change.

**Bad:** `UserManager` handles authentication, profile management, and email notifications.  
**Good:** Split into `AuthService`, `ProfileService`, `NotificationService`.

#### O — Open/Closed Principle (OCP)
> Open for extension, closed for modification.

**How:** Use interfaces/abstract classes. New behavior = new class implementing the interface.

```
// Bad: modify PaymentProcessor every time you add a payment method
class PaymentProcessor:
    processPayment(type):
        if type == "credit": ...
        elif type == "debit": ...
        elif type == "crypto": ...  // keep adding here

// Good: extend via new implementations
interface PaymentMethod:
    process(amount)

class CreditPayment implements PaymentMethod: ...
class DebitPayment implements PaymentMethod: ...
class CryptoPayment implements PaymentMethod: ...  // add without modifying existing code
```

#### L — Liskov Substitution Principle (LSP)
> Subtypes must be substitutable for their base types without breaking correctness.

**Classic violation:** `Square extends Rectangle` — setting width on a Square unexpectedly changes height.  
**Fix:** If a subclass can't honor the contract of the parent, don't use inheritance.

#### I — Interface Segregation Principle (ISP)
> No client should be forced to depend on methods it doesn't use.

**Bad:** One giant `Vehicle` interface with `fly()`, `drive()`, `sail()`.  
**Good:** Separate `Flyable`, `Drivable`, `Sailable` interfaces.

#### D — Dependency Inversion Principle (DIP)
> High-level modules should not depend on low-level modules. Both should depend on abstractions.

```
// Bad: Game directly creates and depends on ConsoleRenderer
class Game:
    renderer = ConsoleRenderer()

// Good: Game depends on Renderer interface
class Game:
    renderer: Renderer  // injected — could be ConsoleRenderer, WebRenderer, etc.
```

---

## 4. OOP Concepts

### Encapsulation
> Bundle data and methods that operate on that data within a single class. Hide internal state.

- Fields are **private**
- Access through **public methods** (getters/setters only if needed)
- Internal representation can change without affecting callers

```
class BankAccount:
    private balance: float
    
    deposit(amount):
        if amount > 0:
            balance += amount
    
    getBalance() → float:
        return balance
```

### Abstraction
> Expose only what's necessary. Hide complexity behind simple interfaces.

- Callers don't need to know *how*, only *what*
- Abstract classes / interfaces define the "what"
- Concrete classes implement the "how"

### Polymorphism
> Same interface, different behavior depending on the concrete type.

```
interface Shape:
    area() → float

class Circle implements Shape:
    area() → π * r²

class Rectangle implements Shape:
    area() → width * height

// Code that uses Shape doesn't care which one it is
totalArea = sum(shape.area() for shape in shapes)
```

### Inheritance vs Composition

| Aspect | Inheritance (is-a) | Composition (has-a) |
|--------|-------------------|---------------------|
| Coupling | Tight — child depends on parent internals | Loose — delegates to composed objects |
| Flexibility | Rigid — can't change parent at runtime | Flexible — swap components at runtime |
| When to use | True is-a relationship, want to reuse interface | Want to reuse behavior, combine capabilities |

> **Default to composition.** Use inheritance only for true type hierarchies (e.g., `Dog is-a Animal`).

---

## 5. Design Patterns

### Creational Patterns

#### Factory Method
> Create objects without specifying the exact class. Delegate creation to a factory.

**When:** You need to create objects of different types based on some condition.

```
class NotificationFactory:
    static create(type: String) → Notification:
        if type == "email": return EmailNotification()
        if type == "sms": return SMSNotification()
        if type == "push": return PushNotification()
```

**Use in LLD:** Creating different vehicle types in Parking Lot, different piece types in Chess.

#### Builder
> Construct complex objects step by step. Separate construction from representation.

**When:** Object has many optional parameters or complex construction logic.

```
class QueryBuilder:
    select(columns) → self
    from(table) → self
    where(condition) → self
    build() → Query
    
query = QueryBuilder().select("*").from("users").where("age > 18").build()
```

**Use in LLD:** Building complex configuration objects, multi-step order creation.

#### Singleton
> Ensure a class has only one instance globally.

**When:** Exactly one instance makes sense (config manager, connection pool, logger).

```
class ConfigManager:
    private static instance: ConfigManager
    
    static getInstance():
        if instance is null:
            instance = ConfigManager()
        return instance
```

**Warning:** Singletons make testing harder and introduce global state. Use sparingly.

### Structural Patterns

#### Decorator
> Add behavior to objects dynamically by wrapping them.

**When:** You need to add features to objects without modifying their class.

```
interface Coffee:
    cost() → float
    description() → String

class BasicCoffee implements Coffee:
    cost() → 2.0

class MilkDecorator implements Coffee:
    wrapped: Coffee
    cost() → wrapped.cost() + 0.5

class SugarDecorator implements Coffee:
    wrapped: Coffee
    cost() → wrapped.cost() + 0.3

// Usage
coffee = SugarDecorator(MilkDecorator(BasicCoffee()))
coffee.cost()  // 2.8
```

**Use in LLD:** Pizza toppings, notification channels, log formatting.

#### Facade
> Provide a simplified interface to a complex subsystem.

**When:** A subsystem has many classes but callers only need a simple API.

```
class OrderFacade:
    inventoryService: InventoryService
    paymentService: PaymentService
    shippingService: ShippingService
    
    placeOrder(order):
        inventoryService.reserve(order.items)
        paymentService.charge(order.total)
        shippingService.schedule(order)
```

**Use in LLD:** Simplifying complex subsystems (payment processing, booking systems).

### Behavioral Patterns

#### Strategy
> Define a family of algorithms. Encapsulate each one and make them interchangeable.

**When:** You need to switch between different algorithms/approaches at runtime.

```
interface PricingStrategy:
    calculatePrice(basePrice) → float

class RegularPricing implements PricingStrategy:
    calculatePrice(basePrice) → basePrice

class PremiumPricing implements PricingStrategy:
    calculatePrice(basePrice) → basePrice * 0.8

class ShoppingCart:
    pricingStrategy: PricingStrategy
    
    checkout():
        for item in items:
            price = pricingStrategy.calculatePrice(item.basePrice)
```

**Use in LLD:** Elevator scheduling algorithms, parking spot allocation, pricing tiers.

#### Observer
> When one object changes state, all dependents are notified automatically.

**When:** Multiple objects need to react to changes in another object.

```
interface Observer:
    update(event)

class EventBus:
    subscribers: Map<EventType, List<Observer>>
    
    subscribe(eventType, observer)
    publish(eventType, event):
        for observer in subscribers[eventType]:
            observer.update(event)
```

**Use in LLD:** Notification systems, scoreboard updates, real-time dashboards.

#### State Machine
> Object behavior changes based on its internal state. Each state is a separate class.

**When:** An entity goes through well-defined states with specific transitions.

```
interface OrderState:
    cancel(order)
    ship(order)
    deliver(order)

class PendingState implements OrderState:
    cancel(order) → order.setState(CancelledState())
    ship(order) → order.setState(ShippedState())
    deliver(order) → throw InvalidTransition

class ShippedState implements OrderState:
    cancel(order) → throw InvalidTransition
    ship(order) → throw InvalidTransition
    deliver(order) → order.setState(DeliveredState())
```

**Use in LLD:** Order lifecycle, elevator states, vending machine, traffic light.

---

## 6. Concurrency

### Core Concepts

| Concept | Description |
|---------|-------------|
| **Thread Safety** | Code that functions correctly when accessed by multiple threads simultaneously |
| **Race Condition** | Bug where behavior depends on thread timing/ordering |
| **Deadlock** | Two+ threads each waiting for the other to release a resource |
| **Mutex / Lock** | Ensures only one thread can access a critical section at a time |
| **Semaphore** | Allows N threads to access a resource concurrently |
| **Atomic Operations** | Indivisible operations that complete without interruption |

### Common Patterns

| Pattern | When to Use | Example |
|---------|-------------|---------|
| **Lock / Mutex** | Protecting shared mutable state | Updating account balance |
| **Read-Write Lock** | Many readers, few writers | Cache with infrequent updates |
| **Producer-Consumer** | Decouple production from consumption | Task queue, message broker |
| **Thread Pool** | Reuse threads for many short-lived tasks | Web server request handling |
| **Compare-and-Swap (CAS)** | Lock-free updates to single variables | Atomic counters, lock-free queues |

### When It Matters in LLD

- **Parking Lot:** Multiple cars entering/exiting simultaneously → lock on spot assignment
- **Elevator:** Multiple requests arriving concurrently → thread-safe request queue
- **Rate Limiter:** Concurrent API calls → atomic counter or synchronized sliding window
- **Inventory Management:** Concurrent stock updates → optimistic locking or CAS
- **Booking Systems:** Double-booking prevention → pessimistic locking on seats/slots

---

# Problem Breakdowns

> Each problem follows the 5-Step Delivery Framework.
> Template for each problem is given below — implement each one using this structure.

---

## Template

```
## [Problem Name]
Difficulty: [Easy/Medium/Hard] | Companies: [Company1, Company2]

### Step 1: Requirements
#### Clarifying Questions
1. Q: ...? A: ...
2. Q: ...? A: ...
3. Q: ...? A: ...

#### Final Requirements
1. ...
2. ...
3. ...
4. ...

### Step 2: Core Entities
- **Entity1** — description
- **Entity2** — description
- **Entity3** — description

Relationships:
- Entity1 has-a Entity2
- Entity1 uses Entity3

### Step 3: Class Design

class Entity1:
    // State
    attr1: Type
    attr2: Type
    
    // Behavior
    method1(params) → ReturnType
    method2(params) → ReturnType

class Entity2:
    ...

### Step 4: Implementation
[Pseudocode for core flows + verification trace]

### Step 5: Extensibility
1. "What if ...?" → Approach: ...
2. "What if ...?" → Approach: ...
3. "What if ...?" → Approach: ...

### Level Expectations
| Level | Expectations |
|-------|-------------|
| Junior | Core class structure, basic happy path |
| Mid | Clean design, handles edge cases, uses patterns where appropriate |
| Senior | Extensible design, concurrency considerations, discusses trade-offs |
```

---

## Easy

---

### 1. Tic-Tac-Toe

**Difficulty:** Easy | **Companies:** Google, Amazon, Meta

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Standard 3×3 board or configurable? A: Standard 3×3
2. Q: Two human players or AI opponent? A: Two human players
3. Q: Any special rules beyond standard? A: No, standard rules

##### Final Requirements
1. Two players take turns placing X and O on a 3×3 grid
2. First player to get 3 in a row (horizontal, vertical, diagonal) wins
3. If all cells filled with no winner → draw
4. Cannot place on already-occupied cell

#### Step 2: Core Entities
- **Game** — orchestrates the game loop, tracks status
- **Board** — holds the 3×3 grid, handles placement and win detection
- **Player** — represents a player with a name and symbol (X/O)

#### Step 3: Class Design

```
enum Symbol { X, O, EMPTY }
enum GameStatus { IN_PROGRESS, WIN, DRAW }

class Player:
    name: String
    symbol: Symbol

class Board:
    grid: Symbol[3][3]  // initialized to EMPTY
    moveCount: int
    
    placePiece(row: int, col: int, symbol: Symbol) → bool
    checkWinner() → Symbol?        // returns winning symbol or null
    isFull() → bool                // moveCount == 9

class Game:
    board: Board
    players: Player[2]
    currentPlayerIndex: int
    status: GameStatus
    
    makeMove(row: int, col: int) → MoveResult
    switchTurn()
    getWinner() → Player?
```

#### Step 4: Implementation

```
Game.makeMove(row, col):
    if status != IN_PROGRESS:
        return GAME_OVER
    
    currentPlayer = players[currentPlayerIndex]
    
    if not board.placePiece(row, col, currentPlayer.symbol):
        return INVALID_MOVE
    
    winner = board.checkWinner()
    if winner != null:
        status = WIN
        return WIN
    
    if board.isFull():
        status = DRAW
        return DRAW
    
    switchTurn()
    return SUCCESS

Board.checkWinner():
    // Check rows
    for row in 0..2:
        if grid[row][0] == grid[row][1] == grid[row][2] != EMPTY:
            return grid[row][0]
    // Check columns
    for col in 0..2:
        if grid[0][col] == grid[1][col] == grid[2][col] != EMPTY:
            return grid[0][col]
    // Check diagonals
    if grid[0][0] == grid[1][1] == grid[2][2] != EMPTY: return grid[0][0]
    if grid[0][2] == grid[1][1] == grid[2][0] != EMPTY: return grid[0][2]
    return null
```

**Verification Trace:**
```
Move 1: X at (0,0) → [X,_,_; _,_,_; _,_,_] → no winner → switch to O
Move 2: O at (1,1) → [X,_,_; _,O,_; _,_,_] → no winner → switch to X
Move 3: X at (0,1) → [X,X,_; _,O,_; _,_,_] → no winner → switch to O
Move 4: O at (2,2) → [X,X,_; _,O,_; _,_,O] → no winner → switch to X
Move 5: X at (0,2) → [X,X,X; _,O,_; _,_,O] → X wins! ✓
```

#### Step 5: Extensibility
1. **"What if we want N×N board with K-in-a-row?"** → Parameterize `Board(size, winLength)`. Generalize `checkWinner()` loop bounds.
2. **"What if we add an AI player?"** → Extract `Player` to interface with `getMove(board) → (row, col)`. `HumanPlayer` prompts, `AIPlayer` computes.
3. **"What if we want undo?"** → Add move history stack to Game. `undo()` pops last move, reverts board cell, switches turn back.

---

### 2. Connect Four

**Difficulty:** Easy | **Companies:** Amazon, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Standard 6×7 board? A: Yes
2. Q: Two human players? A: Yes
3. Q: Standard rules — 4 in a row to win? A: Yes

##### Final Requirements
1. Two players alternate dropping Red/Yellow discs into a 7-column, 6-row board
2. Disc falls to the lowest available row in the chosen column
3. First player to get 4 in a row (horizontal, vertical, diagonal) wins
4. If all cells filled with no winner → draw
5. Cannot drop into a full column

#### Step 2: Core Entities
- **Game** — orchestrates turns, tracks status
- **Board** — 6×7 grid, handles disc dropping and win detection
- **Player** — has a name and disc color

#### Step 3: Class Design

```
enum Disc { RED, YELLOW, EMPTY }
enum GameStatus { IN_PROGRESS, WIN, DRAW }

class Player:
    name: String
    disc: Disc

class Board:
    grid: Disc[6][7]        // [row][col], row 0 = top
    colHeights: int[7]      // tracks next available row per column
    
    dropDisc(col: int, disc: Disc) → (row, col)?  // returns position or null if full
    checkWinner(lastRow: int, lastCol: int) → bool // check from last placed piece
    isFull() → bool

class Game:
    board: Board
    players: Player[2]
    currentPlayerIndex: int
    status: GameStatus
    
    makeMove(col: int) → MoveResult
    switchTurn()
```

#### Step 4: Implementation

```
Board.dropDisc(col, disc):
    if colHeights[col] >= 6:
        return null  // column full
    row = 5 - colHeights[col]  // bottom-up fill
    grid[row][col] = disc
    colHeights[col]++
    return (row, col)

Board.checkWinner(row, col):
    disc = grid[row][col]
    directions = [(0,1), (1,0), (1,1), (1,-1)]  // horizontal, vertical, 2 diagonals
    for (dr, dc) in directions:
        count = 1
        // Count in positive direction
        for i in 1..3:
            r, c = row + dr*i, col + dc*i
            if inBounds(r, c) and grid[r][c] == disc: count++
            else: break
        // Count in negative direction
        for i in 1..3:
            r, c = row - dr*i, col - dc*i
            if inBounds(r, c) and grid[r][c] == disc: count++
            else: break
        if count >= 4: return true
    return false

Game.makeMove(col):
    if status != IN_PROGRESS: return GAME_OVER
    
    result = board.dropDisc(col, players[currentPlayerIndex].disc)
    if result is null: return INVALID_MOVE
    
    (row, placedCol) = result
    if board.checkWinner(row, placedCol):
        status = WIN
        return WIN
    if board.isFull():
        status = DRAW
        return DRAW
    
    switchTurn()
    return SUCCESS
```

#### Step 5: Extensibility
1. **"What if board size is configurable?"** → Parameterize `Board(rows, cols, winLength)`.
2. **"What if we add gravity variants (e.g., pop out bottom disc)?"** → New method `popDisc(col)` shifts column up, re-checks win conditions.
3. **"What if we want an AI opponent?"** → Strategy pattern: `MoveStrategy` interface with `HumanStrategy` and `AIStrategy` (minimax).

---

### 3. Amazon Locker

**Difficulty:** Easy | **Companies:** Amazon

#### Step 1: Requirements

##### Clarifying Questions
1. Q: What sizes of lockers? A: Small, Medium, Large
2. Q: Pickup and dropoff, or just one direction? A: Both
3. Q: Multiple locker locations? A: Yes
4. Q: How does user authenticate? A: 6-digit access code

##### Final Requirements
1. System manages multiple locker locations, each with compartments of various sizes (S/M/L)
2. Packages are assigned to the smallest available compartment that fits them
3. Delivery driver deposits package → system generates a 6-digit access code
4. Customer enters code → compartment opens → package is retrieved
5. Access codes expire after a configurable time window

#### Step 2: Core Entities
- **LockerSystem** — manages all locker locations
- **LockerLocation** — a physical location containing compartments
- **Compartment** — individual locker slot with a size and locked/unlocked state
- **Package** — item with dimensions/size category
- **AccessCode** — generated code with expiration

#### Step 3: Class Design

```
enum Size { SMALL, MEDIUM, LARGE }
enum CompartmentStatus { AVAILABLE, OCCUPIED }

class Package:
    id: String
    size: Size

class Compartment:
    id: String
    size: Size
    status: CompartmentStatus
    currentPackage: Package?
    
    isAvailable() → bool
    storePackage(package: Package)
    removePackage() → Package

class AccessCode:
    code: String          // 6-digit
    compartmentId: String
    locationId: String
    expiresAt: DateTime
    
    isValid() → bool      // not expired

class LockerLocation:
    id: String
    address: String
    compartments: List<Compartment>
    
    findAvailableCompartment(size: Size) → Compartment?  // smallest that fits
    getCompartment(id: String) → Compartment?

class LockerSystem:
    locations: Map<String, LockerLocation>
    activeCodes: Map<String, AccessCode>     // code → AccessCode
    
    deposit(locationId, package) → AccessCode?
    pickup(code: String) → Package?
```

#### Step 4: Implementation

```
LockerSystem.deposit(locationId, package):
    location = locations[locationId]
    compartment = location.findAvailableCompartment(package.size)
    if compartment is null: return null  // no space
    
    compartment.storePackage(package)
    code = generateUniqueCode()  // random 6-digit, ensure uniqueness
    accessCode = AccessCode(code, compartment.id, locationId, now() + 3 days)
    activeCodes[code] = accessCode
    return accessCode

LockerSystem.pickup(code):
    accessCode = activeCodes[code]
    if accessCode is null or not accessCode.isValid():
        return null  // invalid or expired
    
    location = locations[accessCode.locationId]
    compartment = location.getCompartment(accessCode.compartmentId)
    package = compartment.removePackage()
    activeCodes.remove(code)
    return package

LockerLocation.findAvailableCompartment(size):
    // Find smallest available compartment that fits
    // Size ordering: SMALL < MEDIUM < LARGE
    for candidateSize in [size, next sizes...]:
        for compartment in compartments:
            if compartment.size == candidateSize and compartment.isAvailable():
                return compartment
    return null
```

#### Step 5: Extensibility
1. **"What if codes expire and package needs to be returned?"** → Background job checks expired codes, marks compartment for return. `ReturnService` handles logistics.
2. **"What if we add notifications?"** → Observer pattern: `LockerSystem` publishes events (`PackageDeposited`, `CodeExpiring`). `NotificationService` subscribes.
3. **"What if we want dynamic pricing based on compartment size/duration?"** → Strategy pattern: `PricingStrategy` interface with `FlatPricing`, `SizeTieredPricing`, `DurationBasedPricing`.

---

### 4. Vending Machine

**Difficulty:** Easy | **Companies:** Google, Microsoft, Amazon

#### Step 1: Requirements

##### Clarifying Questions
1. Q: What payment methods? A: Coins and bills (cash only for simplicity)
2. Q: Multiple products per machine? A: Yes, each slot has a product and quantity
3. Q: Does the machine give change? A: Yes

##### Final Requirements
1. Machine holds multiple products in slots, each with a price and quantity
2. User inserts money (coins/bills), selects a product
3. If enough money inserted and product in stock → dispense product and return change
4. If not enough money → prompt for more or allow cancel/refund
5. Machine tracks inventory and cash balance

#### Step 2: Core Entities
- **VendingMachine** — main controller, manages state
- **Slot** — holds a product with quantity and price
- **Product** — name, price
- **CashRegister** — tracks inserted money, makes change

#### Step 3: Class Design

```
enum MachineState { IDLE, ACCEPTING_MONEY, DISPENSING }

class Product:
    name: String
    price: float

class Slot:
    code: String        // e.g., "A1"
    product: Product
    quantity: int
    
    isAvailable() → bool
    dispense() → Product

class CashRegister:
    insertedAmount: float
    
    insertMoney(amount: float)
    getInsertedAmount() → float
    calculateChange(price: float) → float
    reset()

class VendingMachine:
    slots: Map<String, Slot>
    cashRegister: CashRegister
    state: MachineState
    
    insertMoney(amount: float)
    selectProduct(slotCode: String) → DispenseResult
    cancelAndRefund() → float
```

#### Step 4: Implementation

```
VendingMachine.selectProduct(slotCode):
    slot = slots[slotCode]
    if slot is null: return INVALID_SLOT
    if not slot.isAvailable(): return OUT_OF_STOCK
    
    price = slot.product.price
    inserted = cashRegister.getInsertedAmount()
    
    if inserted < price:
        return INSUFFICIENT_FUNDS(price - inserted)
    
    product = slot.dispense()
    change = cashRegister.calculateChange(price)
    cashRegister.reset()
    state = IDLE
    return SUCCESS(product, change)

VendingMachine.cancelAndRefund():
    refund = cashRegister.getInsertedAmount()
    cashRegister.reset()
    state = IDLE
    return refund
```

#### Step 5: Extensibility
1. **"What if we add card payment?"** → Strategy pattern: `PaymentMethod` interface. `CashPayment`, `CardPayment` implementations. `VendingMachine` delegates to selected payment strategy.
2. **"What if we want admin to restock remotely?"** → Add `AdminService` with `restock(slotCode, quantity)` and `updatePrice(slotCode, price)`.
3. **"What if we add state machine with explicit transitions?"** → State pattern: `IdleState`, `AcceptingMoneyState`, `DispensingState` classes. Each state handles valid actions and transitions.

---

### 5. Snake and Ladder

**Difficulty:** Easy | **Companies:** Google, Amazon, Flipkart

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Standard 10×10 board (100 cells)? A: Yes
2. Q: How many players? A: 2–4 players
3. Q: Single die or two dice? A: Single 6-sided die
4. Q: Configurable snake/ladder positions? A: Yes

##### Final Requirements
1. 2–4 players take turns rolling a 6-sided die and advancing on a 10×10 board
2. Landing on a ladder bottom → move up to ladder top
3. Landing on a snake head → move down to snake tail
4. First player to reach or exceed position 100 wins
5. Snake/ladder positions are configurable at game setup

#### Step 2: Core Entities
- **Game** — orchestrates turns, tracks winner
- **Board** — holds snakes and ladders, resolves final position
- **Player** — has a name and position
- **Dice** — generates random roll
- **Snake** — has head and tail positions
- **Ladder** — has bottom and top positions

#### Step 3: Class Design

```
class Dice:
    roll() → int  // 1-6

class Player:
    name: String
    position: int  // starts at 0

class Board:
    size: int  // 100
    jumps: Map<int, int>  // position → destination (covers both snakes and ladders)
    
    getFinalPosition(position: int) → int  // resolve snakes/ladders
    isWinningPosition(position: int) → bool

class Game:
    board: Board
    players: List<Player>
    dice: Dice
    currentPlayerIndex: int
    winner: Player?
    
    playTurn() → TurnResult
    isGameOver() → bool
```

#### Step 4: Implementation

```
Game.playTurn():
    player = players[currentPlayerIndex]
    roll = dice.roll()
    newPosition = player.position + roll
    
    if newPosition > board.size:
        // Don't move if overshoot (some variants)
        switchTurn()
        return OVERSHOOT
    
    newPosition = board.getFinalPosition(newPosition)
    player.position = newPosition
    
    if board.isWinningPosition(newPosition):
        winner = player
        return WIN(player)
    
    switchTurn()
    return MOVED(player, roll, newPosition)

Board.getFinalPosition(position):
    // Resolve chains: ladder to snake, etc.
    while position in jumps:
        position = jumps[position]
    return position
```

#### Step 5: Extensibility
1. **"What if we add power-ups on certain cells?"** → Generalize `jumps` to `cellEffects: Map<int, CellEffect>`. `CellEffect` interface with `SnakeEffect`, `LadderEffect`, `PowerUpEffect`.
2. **"What if we want 2 dice?"** → Parameterize `Dice(count)` or create `DicePair`. Modify `roll()` accordingly.
3. **"What if we add multiplayer online?"** → Extract `GameServer` that receives moves over network, validates them, broadcasts state updates (Observer).

---

## Medium

---

### 6. Parking Lot

**Difficulty:** Medium | **Companies:** Amazon, Google, Microsoft, Uber

#### Step 1: Requirements

##### Clarifying Questions
1. Q: What vehicle types? A: Motorcycle, Car, Truck
2. Q: Multiple floors/levels? A: Yes, multi-level garage
3. Q: Different spot sizes? A: Small (motorcycle), Medium (car), Large (truck)
4. Q: Payment system? A: Pay-per-hour when exiting
5. Q: Multiple entry/exit points? A: Yes

##### Final Requirements
1. Multi-level parking garage with spots of 3 sizes: Small, Medium, Large
2. Vehicles are assigned to the smallest available spot that fits them
3. System tracks entry time; payment calculated on exit (hourly rate)
4. Multiple entry and exit points
5. Display available spot count per level

#### Step 2: Core Entities
- **ParkingLot** — top-level manager
- **Level** — a floor in the garage
- **ParkingSpot** — individual spot with size
- **Vehicle** — motorcycle, car, or truck
- **Ticket** — records entry time and assigned spot
- **PaymentProcessor** — calculates and processes fees

#### Step 3: Class Design

```
enum VehicleType { MOTORCYCLE, CAR, TRUCK }
enum SpotSize { SMALL, MEDIUM, LARGE }
enum SpotStatus { AVAILABLE, OCCUPIED }

class Vehicle:
    licensePlate: String
    type: VehicleType

class ParkingSpot:
    id: String
    size: SpotSize
    status: SpotStatus
    level: int
    
    canFit(vehicle: Vehicle) → bool
    occupy()
    vacate()

class Ticket:
    id: String
    vehicle: Vehicle
    spot: ParkingSpot
    entryTime: DateTime
    exitTime: DateTime?

class Level:
    levelNumber: int
    spots: List<ParkingSpot>
    availableCount: Map<SpotSize, int>
    
    findAvailableSpot(vehicleType: VehicleType) → ParkingSpot?

class ParkingLot:
    levels: List<Level>
    activeTickets: Map<String, Ticket>  // licensePlate → Ticket
    
    enter(vehicle: Vehicle) → Ticket?
    exit(licensePlate: String) → Payment?
    getAvailability() → Map<int, Map<SpotSize, int>>

class PaymentProcessor:
    rates: Map<SpotSize, float>  // hourly rate per spot size
    
    calculateFee(ticket: Ticket) → float
```

#### Step 4: Implementation

```
ParkingLot.enter(vehicle):
    for level in levels:
        spot = level.findAvailableSpot(vehicle.type)
        if spot != null:
            spot.occupy()
            ticket = Ticket(generateId(), vehicle, spot, now())
            activeTickets[vehicle.licensePlate] = ticket
            return ticket
    return null  // lot full

ParkingLot.exit(licensePlate):
    ticket = activeTickets[licensePlate]
    if ticket is null: return null
    
    ticket.exitTime = now()
    fee = paymentProcessor.calculateFee(ticket)
    ticket.spot.vacate()
    activeTickets.remove(licensePlate)
    return Payment(fee, ticket)

Level.findAvailableSpot(vehicleType):
    requiredSize = mapVehicleToMinSpotSize(vehicleType)
    for size in [requiredSize, ...larger sizes]:
        for spot in spots:
            if spot.size == size and spot.status == AVAILABLE:
                return spot
    return null

PaymentProcessor.calculateFee(ticket):
    hours = ceil((ticket.exitTime - ticket.entryTime) / 1 hour)
    return hours * rates[ticket.spot.size]
```

#### Step 5: Extensibility
1. **"What if we add EV charging spots?"** → Add `SpotType` enum (REGULAR, EV_CHARGING). `ParkingSpot` gets a `type` field. `findAvailableSpot` can filter by type.
2. **"What if we add reservation system?"** → New `Reservation` entity with time slot. `Level.findAvailableSpot` checks against reservations. Add `reserve()` and `cancelReservation()` to ParkingLot.
3. **"What if we add dynamic pricing?"** → Strategy pattern: `PricingStrategy` interface. `FlatRate`, `PeakHourRate`, `DurationTiered` implementations. PaymentProcessor uses injected strategy.

**Concurrency Note:** `enter()` and `exit()` must be thread-safe. Use locks on `findAvailableSpot` + `occupy()` to prevent double-assignment.

---

### 7. Elevator System

**Difficulty:** Medium | **Companies:** Microsoft, Google, Amazon

#### Step 1: Requirements

##### Clarifying Questions
1. Q: How many elevators? A: Multiple (e.g., 4)
2. Q: How many floors? A: Configurable (e.g., 20)
3. Q: Scheduling algorithm? A: SCAN (elevator algorithm) — move in one direction, serve all requests, then reverse
4. Q: Weight limits? A: Not for now

##### Final Requirements
1. Multiple elevators serve a building with N floors
2. Users request an elevator from a floor (with desired direction)
3. Users select destination floor once inside
4. Elevators follow the SCAN algorithm: continue in current direction, serve all requests, then reverse
5. System assigns the optimal elevator to each external request

#### Step 2: Core Entities
- **ElevatorController** — receives requests, dispatches to elevators
- **Elevator** — moves between floors, serves requests
- **Request** — external (floor + direction) or internal (destination floor)

#### Step 3: Class Design

```
enum Direction { UP, DOWN, IDLE }

class Request:
    floor: int
    direction: Direction?  // null for internal requests

class Elevator:
    id: int
    currentFloor: int
    direction: Direction
    upStops: SortedSet<int>      // floors to visit going up
    downStops: SortedSet<int>    // floors to visit going down
    
    addStop(floor: int)
    move()                       // move one floor in current direction
    shouldStop() → bool          // check if current floor is a stop
    openDoors()
    
class ElevatorController:
    elevators: List<Elevator>
    
    requestElevator(floor: int, direction: Direction)  // external request
    selectFloor(elevatorId: int, floor: int)           // internal request
    findBestElevator(floor: int, direction: Direction) → Elevator
    step()                                              // advance simulation one tick
```

#### Step 4: Implementation

```
ElevatorController.requestElevator(floor, direction):
    elevator = findBestElevator(floor, direction)
    elevator.addStop(floor)

ElevatorController.findBestElevator(floor, direction):
    bestElevator = null
    bestScore = MAX_INT
    for elevator in elevators:
        score = calculateScore(elevator, floor, direction)
        if score < bestScore:
            bestScore = score
            bestElevator = elevator
    return bestElevator

// Score: prefer elevator already moving toward the floor in the right direction
calculateScore(elevator, floor, direction):
    if elevator.direction == IDLE:
        return abs(elevator.currentFloor - floor)
    if elevator.direction == direction:
        if (direction == UP and elevator.currentFloor <= floor) or
           (direction == DOWN and elevator.currentFloor >= floor):
            return abs(elevator.currentFloor - floor)  // on the way
    return MAX_FLOORS + abs(elevator.currentFloor - floor)  // will need to reverse

Elevator.move():
    if direction == UP:
        if upStops is not empty:
            currentFloor++
            if shouldStop(): openDoors(); upStops.remove(currentFloor)
        else: direction = DOWN  // reverse
    elif direction == DOWN:
        if downStops is not empty:
            currentFloor--
            if shouldStop(): openDoors(); downStops.remove(currentFloor)
        else: direction = UP  // reverse
    
    if upStops.isEmpty() and downStops.isEmpty():
        direction = IDLE

Elevator.addStop(floor):
    if floor > currentFloor: upStops.add(floor)
    elif floor < currentFloor: downStops.add(floor)
    
    if direction == IDLE:
        direction = floor > currentFloor ? UP : DOWN
```

#### Step 5: Extensibility
1. **"What if we add VIP/express elevators?"** → Strategy pattern: `ElevatorSchedulingStrategy`. `SCANStrategy`, `ExpressStrategy` (only certain floors). Controller uses strategy based on elevator type.
2. **"What if we add weight limits?"** → Add `currentWeight` to Elevator. `addStop()` checks weight before accepting. Display "overweight" warning.
3. **"What if certain floors are restricted?"** → Add `accessControl: Map<int, AccessLevel>` to Controller. Validate floor access before adding stop.

---

### 8. Library Management System

**Difficulty:** Medium | **Companies:** Amazon, Google, Flipkart

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Single library or multiple branches? A: Single library
2. Q: How many copies per book? A: Multiple copies possible
3. Q: Reservation system? A: Yes, reserve books currently checked out
4. Q: Late fees? A: Yes, daily late fee after due date

##### Final Requirements
1. Library has books, each with possibly multiple copies
2. Members can search, checkout, return, and reserve books
3. Checkout has a due date (e.g., 14 days). Late returns incur daily fees
4. If no copy is available, member can reserve; notified when copy returns
5. Members have a max borrow limit (e.g., 5 books)

#### Step 2: Core Entities
- **Library** — façade for system operations
- **Book** — title, author, ISBN
- **BookCopy** — physical copy of a book with status
- **Member** — library member with borrowed/reserved books
- **Loan** — tracks checkout: member, copy, dates
- **Reservation** — member waiting for a book

#### Step 3: Class Design

```
enum CopyStatus { AVAILABLE, CHECKED_OUT, RESERVED, LOST }

class Book:
    isbn: String
    title: String
    author: String
    copies: List<BookCopy>
    
    getAvailableCopy() → BookCopy?

class BookCopy:
    id: String
    book: Book
    status: CopyStatus

class Member:
    id: String
    name: String
    activeLoans: List<Loan>
    reservations: List<Reservation>
    
    canBorrow() → bool  // activeLoans.size < MAX_LIMIT

class Loan:
    id: String
    member: Member
    copy: BookCopy
    checkoutDate: DateTime
    dueDate: DateTime
    returnDate: DateTime?
    
    isOverdue() → bool
    calculateFee() → float

class Reservation:
    member: Member
    book: Book
    createdAt: DateTime

class Library:
    books: Map<String, Book>         // ISBN → Book
    members: Map<String, Member>
    activeLoans: Map<String, Loan>   // copyId → Loan
    reservations: Map<String, Queue<Reservation>>  // ISBN → queue
    
    search(query: String) → List<Book>
    checkout(memberId, isbn) → Loan?
    returnBook(copyId) → float       // returns fee owed
    reserve(memberId, isbn) → Reservation?
```

#### Step 4: Implementation

```
Library.checkout(memberId, isbn):
    member = members[memberId]
    if not member.canBorrow(): return null
    
    book = books[isbn]
    copy = book.getAvailableCopy()
    if copy is null: return null  // no copies → must reserve
    
    copy.status = CHECKED_OUT
    loan = Loan(generateId(), member, copy, now(), now() + 14 days)
    activeLoans[copy.id] = loan
    member.activeLoans.add(loan)
    return loan

Library.returnBook(copyId):
    loan = activeLoans[copyId]
    loan.returnDate = now()
    fee = loan.calculateFee()
    
    copy = loan.copy
    activeLoans.remove(copyId)
    loan.member.activeLoans.remove(loan)
    
    // Check reservations
    isbn = copy.book.isbn
    if reservations[isbn] is not empty:
        nextReservation = reservations[isbn].dequeue()
        copy.status = RESERVED
        notify(nextReservation.member, "Book available for pickup")
    else:
        copy.status = AVAILABLE
    
    return fee

Loan.calculateFee():
    if returnDate <= dueDate: return 0
    overdueDays = (returnDate - dueDate).days
    return overdueDays * DAILY_FEE_RATE
```

#### Step 5: Extensibility
1. **"What if we add book categories and advanced search?"** → Add `Category` enum/tags to Book. `Library.search()` supports filtering by author, title, category, ISBN.
2. **"What if we add different member tiers?"** → Strategy pattern: `MemberTier` with `Regular`, `Premium`. Each tier defines `maxBooks`, `loanDuration`, `feeRate`.
3. **"What if we add email/SMS notifications?"** → Observer pattern: `NotificationService` subscribes to events (BookReturned, ReservationReady, LoanOverdue).

---

### 9. Hotel Booking System

**Difficulty:** Medium | **Companies:** Amazon, Booking.com, Airbnb

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Single hotel or chain? A: Single hotel
2. Q: Room types? A: Single, Double, Suite
3. Q: Cancellation policy? A: Free cancellation up to 24h before check-in
4. Q: Payment at booking or check-in? A: At booking time

##### Final Requirements
1. Hotel has rooms of different types (Single, Double, Suite) with different prices
2. Guests can search available rooms for a date range
3. Guests can book, confirm (pay), and cancel reservations
4. Double-booking prevention: a room cannot be booked for overlapping dates
5. Free cancellation up to 24 hours before check-in

#### Step 2: Core Entities
- **Hotel** — manages rooms and bookings
- **Room** — physical room with type and number
- **Booking** — reservation linking guest, room, and dates
- **Guest** — person making the booking
- **PaymentService** — handles payment/refund

#### Step 3: Class Design

```
enum RoomType { SINGLE, DOUBLE, SUITE }
enum BookingStatus { CONFIRMED, CANCELLED, COMPLETED }

class Room:
    number: int
    type: RoomType
    pricePerNight: float

class Guest:
    id: String
    name: String
    email: String

class Booking:
    id: String
    guest: Guest
    room: Room
    checkIn: Date
    checkOut: Date
    status: BookingStatus
    totalPrice: float
    
    canCancel() → bool  // now < checkIn - 24h

class Hotel:
    rooms: List<Room>
    bookings: List<Booking>
    
    searchAvailable(checkIn, checkOut, roomType?) → List<Room>
    book(guest, roomNumber, checkIn, checkOut) → Booking?
    cancel(bookingId) → bool
    
    private isRoomAvailable(room, checkIn, checkOut) → bool

class PaymentService:
    charge(guest, amount) → bool
    refund(guest, amount) → bool
```

#### Step 4: Implementation

```
Hotel.searchAvailable(checkIn, checkOut, roomType):
    available = []
    for room in rooms:
        if roomType != null and room.type != roomType: continue
        if isRoomAvailable(room, checkIn, checkOut):
            available.add(room)
    return available

Hotel.isRoomAvailable(room, checkIn, checkOut):
    for booking in bookings:
        if booking.room == room and booking.status == CONFIRMED:
            // Check overlap: not (checkOut <= booking.checkIn or checkIn >= booking.checkOut)
            if checkIn < booking.checkOut and checkOut > booking.checkIn:
                return false  // overlap
    return true

Hotel.book(guest, roomNumber, checkIn, checkOut):
    room = findRoom(roomNumber)
    if not isRoomAvailable(room, checkIn, checkOut): return null
    
    nights = (checkOut - checkIn).days
    totalPrice = nights * room.pricePerNight
    
    if not paymentService.charge(guest, totalPrice): return null
    
    booking = Booking(generateId(), guest, room, checkIn, checkOut, CONFIRMED, totalPrice)
    bookings.add(booking)
    return booking

Hotel.cancel(bookingId):
    booking = findBooking(bookingId)
    if not booking.canCancel(): return false
    
    booking.status = CANCELLED
    paymentService.refund(booking.guest, booking.totalPrice)
    return true
```

#### Step 5: Extensibility
1. **"What if we add seasonal pricing?"** → Strategy pattern: `PricingStrategy` with `FlatRate`, `SeasonalRate`, `DemandBased`. Hotel injects strategy.
2. **"What if we add room amenities/features?"** → Decorator pattern for rooms, or add `amenities: Set<Amenity>` to `Room` and filter in search.
3. **"What if we need to manage multiple hotels?"** → Extract `HotelChain` as a façade managing multiple `Hotel` instances. Add `location` search parameter.

**Concurrency Note:** `book()` must be synchronized — check availability + reserve must be atomic to prevent double-booking.

---

### 10. Movie Ticket Booking (BookMyShow)

**Difficulty:** Medium | **Companies:** Amazon, BookMyShow, Flipkart

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Multiple theatres/screens? A: Yes
2. Q: Seat selection? A: Yes, user picks specific seats
3. Q: Temporary hold while selecting? A: Yes, 10-min hold
4. Q: Multiple showtimes per movie? A: Yes

##### Final Requirements
1. System manages theatres with multiple screens, each screen showing different movies at different times
2. Users can search movies by title/city and view available showtimes
3. Users select seats from a seat map → seats held for 10 minutes during payment
4. After payment, booking is confirmed. Held seats auto-release on timeout
5. No double-booking of seats

#### Step 2: Core Entities
- **BookingSystem** — top-level façade
- **Theatre** — has multiple screens
- **Screen** — has seat layout
- **Show** — movie + screen + time
- **Seat** — individual seat with status
- **Booking** — confirmed reservation
- **SeatHold** — temporary hold during payment

#### Step 3: Class Design

```
enum SeatStatus { AVAILABLE, HELD, BOOKED }
enum SeatType { REGULAR, PREMIUM, VIP }

class Movie:
    id: String
    title: String
    duration: int  // minutes

class Seat:
    id: String
    row: String
    number: int
    type: SeatType

class Show:
    id: String
    movie: Movie
    screen: Screen
    startTime: DateTime
    seatStatuses: Map<String, SeatStatus>  // seatId → status
    seatPrices: Map<SeatType, float>

class Screen:
    id: String
    seats: List<Seat>

class Theatre:
    id: String
    name: String
    city: String
    screens: List<Screen>

class SeatHold:
    holdId: String
    show: Show
    seatIds: List<String>
    expiresAt: DateTime
    userId: String

class Booking:
    id: String
    show: Show
    seatIds: List<String>
    userId: String
    totalPrice: float

class BookingSystem:
    theatres: List<Theatre>
    shows: Map<String, List<Show>>  // movieId → shows
    activeHolds: Map<String, SeatHold>
    
    searchMovies(city, title?) → List<Movie>
    getShows(movieId, city) → List<Show>
    holdSeats(showId, seatIds, userId) → SeatHold?
    confirmBooking(holdId, paymentInfo) → Booking?
    releaseExpiredHolds()
```

#### Step 4: Implementation

```
BookingSystem.holdSeats(showId, seatIds, userId):
    show = findShow(showId)
    
    // Check all seats available (must be atomic)
    for seatId in seatIds:
        if show.seatStatuses[seatId] != AVAILABLE:
            return null  // seat not available
    
    // Hold all seats
    for seatId in seatIds:
        show.seatStatuses[seatId] = HELD
    
    hold = SeatHold(generateId(), show, seatIds, now() + 10 min, userId)
    activeHolds[hold.holdId] = hold
    return hold

BookingSystem.confirmBooking(holdId, paymentInfo):
    hold = activeHolds[holdId]
    if hold is null or now() > hold.expiresAt:
        releaseHold(hold)
        return null  // hold expired
    
    totalPrice = calculatePrice(hold.show, hold.seatIds)
    if not paymentService.charge(paymentInfo, totalPrice):
        return null
    
    // Convert hold to booking
    for seatId in hold.seatIds:
        hold.show.seatStatuses[seatId] = BOOKED
    
    booking = Booking(generateId(), hold.show, hold.seatIds, hold.userId, totalPrice)
    activeHolds.remove(holdId)
    return booking

BookingSystem.releaseExpiredHolds():
    for hold in activeHolds.values():
        if now() > hold.expiresAt:
            for seatId in hold.seatIds:
                hold.show.seatStatuses[seatId] = AVAILABLE
            activeHolds.remove(hold.holdId)
```

#### Step 5: Extensibility
1. **"What if we add discount coupons?"** → Strategy pattern: `DiscountStrategy` with `FlatDiscount`, `PercentDiscount`, `BuyXGetY`. Apply at checkout.
2. **"What if we add food/snack ordering?"** → New `FoodItem` and `FoodOrder` entities. `Booking` optionally contains `FoodOrder`.
3. **"What if multiple cities with different pricing?"** → `Show.seatPrices` already supports per-show pricing. Add city-level pricing strategy.

**Concurrency Note:** `holdSeats()` MUST be atomic — check + hold must use locking to prevent two users holding the same seat.

---

### 11. LRU Cache

**Difficulty:** Medium | **Companies:** Amazon, Google, Meta, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Fixed capacity? A: Yes, configurable at creation
2. Q: Thread-safe? A: For now, single-threaded
3. Q: What operations? A: get(key), put(key, value)
4. Q: Eviction policy? A: Evict least recently used when at capacity

##### Final Requirements
1. Fixed-capacity key-value cache
2. `get(key)` → returns value and marks as recently used, or null if missing
3. `put(key, value)` → inserts/updates entry, marks as recently used
4. When at capacity, `put` evicts the least recently used entry
5. Both operations must be O(1) time

#### Step 2: Core Entities
- **LRUCache** — main cache with get/put operations
- **DoublyLinkedList** — maintains usage order (most recent at head, least at tail)
- **Node** — doubly linked list node holding key-value pair

#### Step 3: Class Design

```
class Node:
    key: K
    value: V
    prev: Node?
    next: Node?

class DoublyLinkedList:
    head: Node  // sentinel
    tail: Node  // sentinel
    
    addToFront(node: Node)
    remove(node: Node)
    removeLast() → Node

class LRUCache<K, V>:
    capacity: int
    map: HashMap<K, Node>
    list: DoublyLinkedList
    
    get(key: K) → V?
    put(key: K, value: V)
```

#### Step 4: Implementation

```
LRUCache.get(key):
    if key not in map: return null
    node = map[key]
    // Move to front (most recently used)
    list.remove(node)
    list.addToFront(node)
    return node.value

LRUCache.put(key, value):
    if key in map:
        node = map[key]
        node.value = value
        list.remove(node)
        list.addToFront(node)
    else:
        if map.size == capacity:
            // Evict LRU
            lruNode = list.removeLast()
            map.remove(lruNode.key)
        node = Node(key, value)
        list.addToFront(node)
        map[key] = node

DoublyLinkedList.addToFront(node):
    node.next = head.next
    node.prev = head
    head.next.prev = node
    head.next = node

DoublyLinkedList.remove(node):
    node.prev.next = node.next
    node.next.prev = node.prev
```

**Verification:**
```
Cache capacity = 2
put(1, "A") → list: [1] map: {1→A}
put(2, "B") → list: [2, 1] map: {1→A, 2→B}
get(1)      → "A", list: [1, 2]
put(3, "C") → evict 2 (LRU), list: [3, 1] map: {1→A, 3→C}
get(2)      → null ✓
```

#### Step 5: Extensibility
1. **"What if we need thread safety?"** → Add read-write lock. `get()` uses read lock, `put()` uses write lock. Or use `ConcurrentHashMap` + synchronized list ops.
2. **"What if we add TTL (expiration)?"** → Add `expiresAt` to Node. Lazy eviction on `get()` (check expiry) + background cleanup job.
3. **"What if we want LFU instead?"** → Replace linked list with frequency buckets: `Map<int, DoublyLinkedList>` + `minFreq` tracker.

---

### 12. Rate Limiter

**Difficulty:** Medium | **Companies:** Google, Amazon, Stripe, Uber

#### Step 1: Requirements

##### Clarifying Questions
1. Q: What algorithm? A: Support multiple — Token Bucket and Sliding Window
2. Q: Per-user or global? A: Per-user (by user ID or API key)
3. Q: Distributed or single-node? A: Single-node for now
4. Q: What happens when rate exceeded? A: Reject with 429 status

##### Final Requirements
1. Rate limit API requests per user/API key
2. Configurable limits (e.g., 100 requests per minute)
3. Support Token Bucket algorithm (bursty) and Sliding Window (smooth)
4. Return allow/deny decision with remaining quota info
5. Thread-safe for concurrent requests

#### Step 2: Core Entities
- **RateLimiter** — interface for rate limiting
- **TokenBucketLimiter** — token bucket implementation
- **SlidingWindowLimiter** — sliding window implementation
- **RateLimiterRegistry** — manages per-user limiters

#### Step 3: Class Design

```
class RateLimitResult:
    allowed: bool
    remainingTokens: int
    retryAfterMs: long?

interface RateLimiter:
    tryAcquire() → RateLimitResult

class TokenBucketLimiter implements RateLimiter:
    maxTokens: int
    currentTokens: float
    refillRate: float          // tokens per second
    lastRefillTime: long
    
    tryAcquire() → RateLimitResult
    private refill()

class SlidingWindowLimiter implements RateLimiter:
    maxRequests: int
    windowSizeMs: long
    requestTimestamps: Deque<long>
    
    tryAcquire() → RateLimitResult

class RateLimiterRegistry:
    limiters: Map<String, RateLimiter>   // userId → limiter
    config: RateLimitConfig
    
    isAllowed(userId: String) → RateLimitResult

class RateLimitConfig:
    algorithm: AlgorithmType    // TOKEN_BUCKET or SLIDING_WINDOW
    maxRequests: int
    windowMs: long
```

#### Step 4: Implementation

```
TokenBucketLimiter.tryAcquire():
    refill()
    if currentTokens >= 1:
        currentTokens -= 1
        return RateLimitResult(true, floor(currentTokens), null)
    else:
        waitTime = (1 - currentTokens) / refillRate * 1000
        return RateLimitResult(false, 0, waitTime)

TokenBucketLimiter.refill():
    now = currentTimeMs()
    elapsed = (now - lastRefillTime) / 1000.0
    currentTokens = min(maxTokens, currentTokens + elapsed * refillRate)
    lastRefillTime = now

SlidingWindowLimiter.tryAcquire():
    now = currentTimeMs()
    windowStart = now - windowSizeMs
    
    // Remove expired timestamps
    while requestTimestamps is not empty and requestTimestamps.peekFirst() < windowStart:
        requestTimestamps.pollFirst()
    
    if requestTimestamps.size() < maxRequests:
        requestTimestamps.addLast(now)
        return RateLimitResult(true, maxRequests - requestTimestamps.size(), null)
    else:
        oldestExpiry = requestTimestamps.peekFirst() + windowSizeMs - now
        return RateLimitResult(false, 0, oldestExpiry)

RateLimiterRegistry.isAllowed(userId):
    if userId not in limiters:
        limiters[userId] = createLimiter(config)
    return limiters[userId].tryAcquire()
```

#### Step 5: Extensibility
1. **"What if we need distributed rate limiting?"** → Use Redis as shared store. `RedisTokenBucket` stores tokens/timestamps in Redis with atomic Lua scripts.
2. **"What if we want different limits per API endpoint?"** → Composite key: `userId + endpoint`. `RateLimitConfig` per endpoint via config map.
3. **"What if we want a fixed window counter (simpler)?"** → New `FixedWindowLimiter` class: counter + window start time. Reset counter when window expires.

---

### 13. Splitwise / Expense Sharing

**Difficulty:** Medium | **Companies:** Google, Amazon, Flipkart

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Split types? A: Equal, exact amounts, percentage-based
2. Q: Group expenses? A: Yes
3. Q: Settle up (simplify debts)? A: Yes
4. Q: Currency? A: Single currency for simplicity

##### Final Requirements
1. Users can add expenses and split among participants (equal, exact, percentage)
2. Users belong to groups for shared expenses
3. System tracks who owes whom and how much (net balances)
4. Settle-up: minimize number of transactions to clear all debts
5. View balance summary per user

#### Step 2: Core Entities
- **ExpenseService** — façade for operations
- **User** — participant
- **Group** — collection of users
- **Expense** — a payment split among users
- **Split** — individual share of an expense
- **BalanceSheet** — tracks pairwise debts

#### Step 3: Class Design

```
enum SplitType { EQUAL, EXACT, PERCENTAGE }

class User:
    id: String
    name: String
    email: String

class Split:
    user: User
    amount: float

class Expense:
    id: String
    description: String
    totalAmount: float
    paidBy: User
    splits: List<Split>
    splitType: SplitType
    group: Group?

class Group:
    id: String
    name: String
    members: List<User>
    expenses: List<Expense>

class BalanceSheet:
    // balances[A][B] = amount A owes B (positive = A owes B)
    balances: Map<String, Map<String, float>>
    
    addDebt(fromUserId, toUserId, amount)
    getBalance(userId) → Map<String, float>  // per-user debts
    simplifyDebts() → List<Transaction>

class ExpenseService:
    users: Map<String, User>
    groups: Map<String, Group>
    balanceSheet: BalanceSheet
    
    addExpense(paidBy, amount, splits, splitType, groupId?) → Expense
    getBalances(userId) → Map<String, float>
    settleUp(userId1, userId2, amount)
```

#### Step 4: Implementation

```
ExpenseService.addExpense(paidById, amount, participants, splitType, groupId):
    paidBy = users[paidById]
    splits = calculateSplits(amount, participants, splitType)
    
    expense = Expense(generateId(), amount, paidBy, splits, splitType)
    
    for split in splits:
        if split.user != paidBy:
            balanceSheet.addDebt(split.user.id, paidBy.id, split.amount)
    
    if groupId: groups[groupId].expenses.add(expense)
    return expense

calculateSplits(amount, participants, splitType):
    if splitType == EQUAL:
        share = amount / participants.size()
        return [Split(user, share) for user in participants]
    elif splitType == EXACT:
        // participants already contain amounts
        validate sum == amount
        return participants
    elif splitType == PERCENTAGE:
        // participants contain percentages
        validate sum == 100
        return [Split(user, amount * pct / 100) for (user, pct) in participants]

BalanceSheet.addDebt(fromId, toId, amount):
    // Net out: if B already owes A, reduce that first
    if balances[toId][fromId] > 0:
        existing = balances[toId][fromId]
        if amount >= existing:
            balances[toId][fromId] = 0
            balances[fromId][toId] += (amount - existing)
        else:
            balances[toId][fromId] -= amount
    else:
        balances[fromId][toId] += amount

BalanceSheet.simplifyDebts():
    // Calculate net balance for each user
    netBalances = {}  // positive = net creditor, negative = net debtor
    for each pair in balances: compute net
    
    // Greedy: match largest debtor with largest creditor
    creditors = sorted users with positive balance (descending)
    debtors = sorted users with negative balance (ascending)
    transactions = []
    
    while creditors and debtors not empty:
        amount = min(creditor.balance, abs(debtor.balance))
        transactions.add(Transaction(debtor, creditor, amount))
        creditor.balance -= amount
        debtor.balance += amount
        // Remove settled users
    
    return transactions
```

#### Step 5: Extensibility
1. **"What if we add recurring expenses?"** → New `RecurringExpense` with frequency. Background job creates `Expense` instances on schedule.
2. **"What if we add multi-currency?"** → Add `currency` to Expense. Use `CurrencyConverter` service to normalize to base currency for balance calculations.
3. **"What if we add expense categories/reports?"** → Add `category` to Expense. `ReportService` aggregates by category, time period, group.

---

### 14. Chess

**Difficulty:** Medium | **Companies:** Amazon, Google, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Standard chess rules? A: Yes
2. Q: Special moves (castling, en passant, pawn promotion)? A: Castling and promotion; skip en passant for now
3. Q: Two human players? A: Yes
4. Q: Check/checkmate detection? A: Yes

##### Final Requirements
1. Two players play standard chess on an 8×8 board
2. Each piece type has its own movement rules (King, Queen, Rook, Bishop, Knight, Pawn)
3. Game detects check, checkmate, and stalemate
4. Support castling and pawn promotion
5. Players alternate turns; cannot move into check

#### Step 2: Core Entities
- **Game** — orchestrates turns, status
- **Board** — 8×8 grid of cells
- **Piece** (abstract) — has color, position, movement rules
  - King, Queen, Rook, Bishop, Knight, Pawn
- **Player** — has color and pieces
- **Move** — from position to position

#### Step 3: Class Design

```
enum Color { WHITE, BLACK }
enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
enum GameStatus { ACTIVE, CHECK, CHECKMATE, STALEMATE, DRAW }

class Position:
    row: int  // 0-7
    col: int  // 0-7

abstract class Piece:
    color: Color
    position: Position
    type: PieceType
    hasMoved: bool
    
    abstract getValidMoves(board: Board) → List<Position>
    canMoveTo(board, target) → bool

class King extends Piece:
    getValidMoves(board) → // 1 square any direction + castling

class Queen extends Piece:
    getValidMoves(board) → // any direction, any distance

class Rook extends Piece:
    getValidMoves(board) → // horizontal/vertical, any distance

class Bishop extends Piece:
    getValidMoves(board) → // diagonal, any distance

class Knight extends Piece:
    getValidMoves(board) → // L-shape (2+1)

class Pawn extends Piece:
    getValidMoves(board) → // forward 1 (or 2 from start), capture diagonal

class Board:
    grid: Piece?[8][8]
    
    getPiece(pos: Position) → Piece?
    movePiece(from, to: Position)
    isPositionUnderAttack(pos, byColor) → bool
    findKing(color) → Position

class Player:
    color: Color
    name: String

class Move:
    piece: Piece
    from: Position
    to: Position
    capturedPiece: Piece?
    isPromotion: bool
    promotedTo: PieceType?

class Game:
    board: Board
    players: Player[2]
    currentTurn: Color
    status: GameStatus
    moveHistory: List<Move>
    
    makeMove(from, to: Position) → MoveResult
    isInCheck(color: Color) → bool
    isCheckmate(color: Color) → bool
    isStalemate(color: Color) → bool
```

#### Step 4: Implementation

```
Game.makeMove(from, to):
    piece = board.getPiece(from)
    if piece is null or piece.color != currentTurn: return INVALID
    if to not in piece.getValidMoves(board): return INVALID
    
    // Simulate move and check if it leaves own king in check
    capturedPiece = board.getPiece(to)
    board.movePiece(from, to)
    
    if isInCheck(currentTurn):
        // Undo move — can't move into check
        board.movePiece(to, from)
        if capturedPiece: board.grid[to] = capturedPiece
        return INVALID_LEAVES_IN_CHECK
    
    // Handle pawn promotion
    if piece is Pawn and (to.row == 0 or to.row == 7):
        promoteTo = promptPromotion()  // Queen default
        board.grid[to] = createPiece(promoteTo, piece.color, to)
    
    piece.hasMoved = true
    move = Move(piece, from, to, capturedPiece)
    moveHistory.add(move)
    
    // Check game status for opponent
    opponent = opposite(currentTurn)
    if isCheckmate(opponent):
        status = CHECKMATE
        return WIN(currentTurn)
    elif isStalemate(opponent):
        status = STALEMATE
        return DRAW
    elif isInCheck(opponent):
        status = CHECK
    
    currentTurn = opponent
    return SUCCESS

Game.isInCheck(color):
    kingPos = board.findKing(color)
    return board.isPositionUnderAttack(kingPos, opposite(color))

Game.isCheckmate(color):
    if not isInCheck(color): return false
    // Try all possible moves for color — if none escape check, it's checkmate
    for piece in getAllPieces(color):
        for move in piece.getValidMoves(board):
            // Simulate and check
            if not leavesInCheck(piece, move): return false
    return true
```

#### Step 5: Extensibility
1. **"What if we add move timer/clock?"** → Add `ChessClock` with per-player remaining time. Decrement on each turn. Timeout = loss.
2. **"What if we add undo?"** → Move history stack already exists. `undo()` pops last move, restores piece positions and captured pieces.
3. **"What if we add AI opponent?"** → Strategy pattern: `MoveSelector` interface. `HumanMoveSelector` takes input, `AIMoveSelector` uses minimax + alpha-beta pruning.

---

### 15. Inventory Management System

**Difficulty:** Medium | **Companies:** Amazon, Walmart, Flipkart

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Single warehouse or multiple? A: Multiple warehouses
2. Q: Track stock levels? A: Yes, real-time counts
3. Q: Reorder alerts? A: Yes, when stock falls below threshold
4. Q: Support batch operations? A: Yes (bulk add/remove)

##### Final Requirements
1. Manage products across multiple warehouses
2. Track stock levels per product per warehouse in real-time
3. Support add stock, remove stock, transfer between warehouses
4. Alert when stock falls below configurable reorder threshold
5. Concurrent stock updates must be handled safely

#### Step 2: Core Entities
- **InventoryService** — top-level operations
- **Product** — item with SKU
- **Warehouse** — physical location with stock
- **StockEntry** — quantity of a product at a warehouse
- **StockAlert** — low-stock notification

#### Step 3: Class Design

```
class Product:
    sku: String
    name: String
    reorderThreshold: int

class Warehouse:
    id: String
    name: String
    location: String

class StockEntry:
    product: Product
    warehouse: Warehouse
    quantity: int           // must be >= 0
    
    add(amount: int)
    remove(amount: int) → bool  // false if insufficient

class InventoryService:
    stock: Map<(sku, warehouseId), StockEntry>
    alertSubscribers: List<StockAlertObserver>
    
    addStock(sku, warehouseId, quantity)
    removeStock(sku, warehouseId, quantity) → bool
    transferStock(sku, fromWarehouse, toWarehouse, quantity) → bool
    getStockLevel(sku, warehouseId?) → int
    getTotalStock(sku) → int
    
    private checkAndAlert(entry: StockEntry)

interface StockAlertObserver:
    onLowStock(product, warehouse, currentQuantity)
```

#### Step 4: Implementation

```
InventoryService.addStock(sku, warehouseId, quantity):
    entry = getOrCreateEntry(sku, warehouseId)
    synchronized(entry):
        entry.add(quantity)

InventoryService.removeStock(sku, warehouseId, quantity):
    entry = stock[(sku, warehouseId)]
    if entry is null: return false
    synchronized(entry):
        if not entry.remove(quantity): return false
        checkAndAlert(entry)
    return true

InventoryService.transferStock(sku, fromId, toId, quantity):
    fromEntry = stock[(sku, fromId)]
    toEntry = getOrCreateEntry(sku, toId)
    
    // Lock both entries in consistent order to prevent deadlocks
    lock1, lock2 = orderByKey(fromEntry, toEntry)
    synchronized(lock1):
        synchronized(lock2):
            if not fromEntry.remove(quantity): return false
            toEntry.add(quantity)
            checkAndAlert(fromEntry)
    return true

InventoryService.checkAndAlert(entry):
    if entry.quantity <= entry.product.reorderThreshold:
        for subscriber in alertSubscribers:
            subscriber.onLowStock(entry.product, entry.warehouse, entry.quantity)
```

#### Step 5: Extensibility
1. **"What if we add auto-reorder?"** → `AutoReorderObserver` implements `StockAlertObserver`. On low stock, creates a purchase order via `PurchaseOrderService`.
2. **"What if we add batch operations?"** → `addStockBatch(List<StockUpdate>)` method. Wrap in transaction for atomicity.
3. **"What if we add stock history/audit log?"** → Observer pattern: `AuditLogObserver` records all stock changes with timestamp, user, quantity delta.

---

### 16. Task/Job Scheduler

**Difficulty:** Medium | **Companies:** Google, Amazon, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: One-time tasks or recurring? A: Both
2. Q: Priority support? A: Yes
3. Q: Concurrent execution? A: Yes, configurable thread pool
4. Q: What if task fails? A: Retry with configurable attempts

##### Final Requirements
1. Schedule tasks to execute at a specific time or after a delay
2. Support recurring tasks (fixed rate, fixed delay)
3. Priority queue — higher priority tasks run first when resources available
4. Configurable thread pool for concurrent task execution
5. Retry failed tasks up to N times with backoff

#### Step 2: Core Entities
- **Scheduler** — manages task lifecycle
- **Task** — unit of work
- **TaskConfig** — scheduling configuration
- **WorkerPool** — thread pool executing tasks

#### Step 3: Class Design

```
enum TaskStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }
enum ScheduleType { ONE_TIME, FIXED_RATE, FIXED_DELAY }

interface Runnable:
    run()

class Task:
    id: String
    runnable: Runnable
    priority: int             // higher = more important
    nextRunTime: DateTime
    scheduleType: ScheduleType
    interval: Duration?       // for recurring
    status: TaskStatus
    retryCount: int
    maxRetries: int
    
    compareTo(other: Task) → int  // by nextRunTime, then priority

class Scheduler:
    taskQueue: PriorityQueue<Task>  // sorted by nextRunTime
    workerPool: WorkerPool
    running: bool
    
    schedule(runnable, delay, priority, maxRetries) → Task
    scheduleRecurring(runnable, interval, scheduleType, priority) → Task
    cancel(taskId) → bool
    start()
    stop()

class WorkerPool:
    threads: Thread[N]
    
    submit(task: Task)
    
    private executeTask(task: Task)
```

#### Step 4: Implementation

```
Scheduler.start():
    running = true
    // Main loop: check for ready tasks
    while running:
        now = currentTime()
        while taskQueue.peek().nextRunTime <= now:
            task = taskQueue.poll()
            workerPool.submit(task)
        sleep(100ms)  // or use condition variable

WorkerPool.executeTask(task):
    task.status = RUNNING
    try:
        task.runnable.run()
        task.status = COMPLETED
        
        // Reschedule recurring tasks
        if task.scheduleType == FIXED_RATE:
            task.nextRunTime = task.nextRunTime + task.interval
            task.status = PENDING
            scheduler.taskQueue.add(task)
        elif task.scheduleType == FIXED_DELAY:
            task.nextRunTime = now() + task.interval
            task.status = PENDING
            scheduler.taskQueue.add(task)
    catch Exception:
        task.retryCount++
        if task.retryCount <= task.maxRetries:
            task.nextRunTime = now() + backoff(task.retryCount)
            task.status = PENDING
            scheduler.taskQueue.add(task)
        else:
            task.status = FAILED

backoff(retryCount):
    return min(2^retryCount * 1000ms, MAX_BACKOFF)
```

#### Step 5: Extensibility
1. **"What if we add task dependencies?"** → DAG-based scheduling. `Task` gets `dependencies: List<Task>`. Task runs only when all deps are COMPLETED.
2. **"What if we add distributed scheduling?"** → Use distributed lock (Redis/ZK). Tasks stored in shared DB. Workers compete to claim tasks.
3. **"What if we add cron expressions?"** → New `CronSchedule` class that parses cron expressions and computes `nextRunTime`.

---

### 17. Pub/Sub Messaging System

**Difficulty:** Medium | **Companies:** Google, Amazon, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Topic-based or queue-based? A: Topic-based pub/sub
2. Q: Multiple subscribers per topic? A: Yes
3. Q: Message persistence? A: In-memory for now
4. Q: Ordering guarantees? A: FIFO per topic

##### Final Requirements
1. Publishers publish messages to named topics
2. Subscribers subscribe to topics and receive all messages published after subscription
3. Multiple subscribers per topic; each gets its own copy
4. Messages are delivered in FIFO order per topic per subscriber
5. Support unsubscribe

#### Step 2: Core Entities
- **MessageBroker** — central pub/sub coordinator
- **Topic** — named channel for messages
- **Publisher** — sends messages to topics
- **Subscriber** — receives messages from topics
- **Message** — payload with metadata

#### Step 3: Class Design

```
class Message:
    id: String
    topic: String
    payload: String
    timestamp: DateTime

interface MessageHandler:
    onMessage(message: Message)

class Subscriber:
    id: String
    handler: MessageHandler
    queue: Queue<Message>   // per-subscriber message queue
    
    notify(message: Message)

class Topic:
    name: String
    subscribers: List<Subscriber>
    
    publish(message: Message)
    addSubscriber(subscriber: Subscriber)
    removeSubscriber(subscriberId: String)

class MessageBroker:
    topics: Map<String, Topic>
    
    createTopic(name: String) → Topic
    publish(topicName: String, payload: String)
    subscribe(topicName: String, subscriber: Subscriber)
    unsubscribe(topicName: String, subscriberId: String)
```

#### Step 4: Implementation

```
MessageBroker.publish(topicName, payload):
    topic = topics[topicName]
    if topic is null: throw TopicNotFound
    message = Message(generateId(), topicName, payload, now())
    topic.publish(message)

Topic.publish(message):
    for subscriber in subscribers:
        subscriber.notify(message)

Subscriber.notify(message):
    queue.enqueue(message)
    // Async processing (separate thread per subscriber)
    processQueue()

Subscriber.processQueue():
    while queue is not empty:
        message = queue.dequeue()
        try:
            handler.onMessage(message)
        catch Exception:
            // Dead letter queue or retry
            deadLetterQueue.add(message)

MessageBroker.subscribe(topicName, subscriber):
    topic = topics[topicName]
    if topic is null: topic = createTopic(topicName)
    topic.addSubscriber(subscriber)

MessageBroker.unsubscribe(topicName, subscriberId):
    topic = topics[topicName]
    if topic != null:
        topic.removeSubscriber(subscriberId)
```

#### Step 5: Extensibility
1. **"What if we add message filtering?"** → Subscriber provides `Filter` predicate. `Topic.publish()` checks filter before notifying.
2. **"What if we need guaranteed delivery?"** → Add ack mechanism. Message stays in queue until subscriber acks. Retry unacked messages.
3. **"What if we add message persistence?"** → `MessageStore` interface: `InMemoryStore`, `DiskStore`, `DatabaseStore`. Topic delegates storage.

---

## Hard

---

### 18. Ride-Sharing System (Uber/Lyft)

**Difficulty:** Hard | **Companies:** Uber, Lyft, Google, Amazon

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Single ride type or multiple? A: Start with standard rides; pool later as extension
2. Q: Real-time matching? A: Yes, match rider with nearest available driver
3. Q: Dynamic pricing? A: Yes, surge pricing based on demand
4. Q: Payment? A: Integrated payment after ride completion

##### Final Requirements
1. Riders request rides with pickup and dropoff locations
2. System matches rider with nearest available driver
3. Drivers can accept or decline ride requests
4. Price estimated before ride, final price calculated using distance + time + surge
5. Riders and drivers can rate each other after ride completion

#### Step 2: Core Entities
- **RideService** — main orchestrator
- **Rider** — requests rides
- **Driver** — accepts and completes rides
- **Ride** — represents a trip from request to completion
- **Location** — lat/lng coordinates
- **PricingEngine** — calculates fare
- **MatchingService** — finds nearest available driver

#### Step 3: Class Design

```
enum RideStatus { REQUESTED, MATCHED, EN_ROUTE, IN_PROGRESS, COMPLETED, CANCELLED }
enum DriverStatus { AVAILABLE, ON_RIDE, OFFLINE }

class Location:
    lat: double
    lng: double
    distanceTo(other: Location) → double

class Rider:
    id: String
    name: String
    rating: float
    paymentMethod: PaymentMethod

class Driver:
    id: String
    name: String
    rating: float
    status: DriverStatus
    currentLocation: Location
    vehicle: Vehicle

class Vehicle:
    licensePlate: String
    model: String
    capacity: int

class Ride:
    id: String
    rider: Rider
    driver: Driver?
    pickup: Location
    dropoff: Location
    status: RideStatus
    estimatedPrice: float
    finalPrice: float?
    startTime: DateTime?
    endTime: DateTime?
    riderRating: int?
    driverRating: int?

class PricingEngine:
    baseFare: float
    perKmRate: float
    perMinRate: float
    surgeMultiplier: float
    
    estimatePrice(pickup, dropoff) → float
    calculateFinalPrice(ride) → float
    updateSurge(demand, supply)

class MatchingService:
    driverIndex: Map<String, Driver>  // available drivers
    
    findNearestDrivers(location, limit) → List<Driver>

class RideService:
    rides: Map<String, Ride>
    matchingService: MatchingService
    pricingEngine: PricingEngine
    
    requestRide(riderId, pickup, dropoff) → Ride
    acceptRide(rideId, driverId)
    startRide(rideId)
    completeRide(rideId) → Payment
    cancelRide(rideId)
    rateRide(rideId, rating, isRiderRating: bool)
```

#### Step 4: Implementation

```
RideService.requestRide(riderId, pickup, dropoff):
    estimatedPrice = pricingEngine.estimatePrice(pickup, dropoff)
    ride = Ride(generateId(), rider, null, pickup, dropoff, REQUESTED, estimatedPrice)
    rides[ride.id] = ride
    
    // Find and notify nearest drivers
    nearbyDrivers = matchingService.findNearestDrivers(pickup, 5)
    for driver in nearbyDrivers:
        notifyDriver(driver, ride)  // push notification
    
    return ride

RideService.acceptRide(rideId, driverId):
    ride = rides[rideId]
    if ride.status != REQUESTED: return  // already matched
    
    ride.driver = getDriver(driverId)
    ride.status = MATCHED
    ride.driver.status = ON_RIDE
    notifyRider(ride.rider, "Driver on the way")

RideService.completeRide(rideId):
    ride = rides[rideId]
    ride.endTime = now()
    ride.status = COMPLETED
    ride.finalPrice = pricingEngine.calculateFinalPrice(ride)
    ride.driver.status = AVAILABLE
    
    payment = paymentService.charge(ride.rider, ride.finalPrice)
    paymentService.payDriver(ride.driver, ride.finalPrice * 0.75)
    return payment

PricingEngine.estimatePrice(pickup, dropoff):
    distance = pickup.distanceTo(dropoff)
    estimatedMinutes = distance / AVG_SPEED * 60
    return (baseFare + distance * perKmRate + estimatedMinutes * perMinRate) * surgeMultiplier

MatchingService.findNearestDrivers(location, limit):
    available = [d for d in driverIndex.values() if d.status == AVAILABLE]
    available.sort(by: d → d.currentLocation.distanceTo(location))
    return available[:limit]
```

#### Step 5: Extensibility
1. **"What if we add ride pooling?"** → New `PooledRide` extending Ride. `MatchingService` groups nearby riders with similar routes. Driver picks up multiple riders.
2. **"What if we add scheduled rides?"** → Add `scheduledTime` to Ride. `SchedulerService` triggers matching at `scheduledTime - buffer`.
3. **"What if we add different vehicle types?"** → `VehicleType` enum (ECONOMY, PREMIUM, SUV). `PricingEngine` uses type-specific rates. Rider selects type in request.

---

### 19. Food Delivery System (DoorDash/Zomato)

**Difficulty:** Hard | **Companies:** DoorDash, Zomato, Swiggy, Uber Eats

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Multiple restaurants? A: Yes
2. Q: Real-time order tracking? A: Yes
3. Q: Delivery agent assignment? A: Nearest available agent
4. Q: Menu management? A: Yes, restaurants manage their own menus

##### Final Requirements
1. Customers browse restaurants, view menus, place orders
2. Restaurants receive orders, confirm, and prepare food
3. System assigns nearest available delivery agent
4. Customer can track order status in real-time
5. Payment processed at order placement

#### Step 2: Core Entities
- **OrderService** — orchestrator
- **Customer** — places orders
- **Restaurant** — has menu, prepares food
- **Menu / MenuItem** — food items with prices
- **Order** — customer order with items
- **DeliveryAgent** — picks up and delivers
- **DeliveryService** — manages agent assignment

#### Step 3: Class Design

```
enum OrderStatus { PLACED, CONFIRMED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED }
enum AgentStatus { AVAILABLE, ON_DELIVERY, OFFLINE }

class MenuItem:
    id: String
    name: String
    price: float
    available: bool

class Restaurant:
    id: String
    name: String
    location: Location
    menu: List<MenuItem>
    
    confirmOrder(orderId)
    markReady(orderId)

class Customer:
    id: String
    name: String
    address: Location

class OrderItem:
    menuItem: MenuItem
    quantity: int

class Order:
    id: String
    customer: Customer
    restaurant: Restaurant
    items: List<OrderItem>
    status: OrderStatus
    agent: DeliveryAgent?
    totalPrice: float
    createdAt: DateTime
    estimatedDeliveryTime: DateTime?

class DeliveryAgent:
    id: String
    name: String
    status: AgentStatus
    currentLocation: Location

class DeliveryService:
    agents: Map<String, DeliveryAgent>
    
    assignAgent(order: Order) → DeliveryAgent?
    findNearestAgent(location: Location) → DeliveryAgent?

class OrderService:
    orders: Map<String, Order>
    deliveryService: DeliveryService
    statusObservers: Map<String, List<OrderObserver>>  // orderId → observers
    
    placeOrder(customerId, restaurantId, items) → Order
    updateStatus(orderId, newStatus)
    trackOrder(orderId) → OrderStatus
```

#### Step 4: Implementation

```
OrderService.placeOrder(customerId, restaurantId, items):
    customer = getCustomer(customerId)
    restaurant = getRestaurant(restaurantId)
    
    orderItems = []
    total = 0
    for (itemId, qty) in items:
        menuItem = restaurant.menu.find(itemId)
        if not menuItem.available: throw ItemUnavailable
        orderItems.add(OrderItem(menuItem, qty))
        total += menuItem.price * qty
    
    order = Order(generateId(), customer, restaurant, orderItems, PLACED, total)
    orders[order.id] = order
    
    paymentService.charge(customer, total)
    notifyRestaurant(restaurant, order)
    return order

OrderService.updateStatus(orderId, newStatus):
    order = orders[orderId]
    order.status = newStatus
    
    if newStatus == CONFIRMED:
        // Assign delivery agent
        agent = deliveryService.assignAgent(order)
        if agent != null:
            order.agent = agent
            agent.status = ON_DELIVERY
    
    if newStatus == DELIVERED:
        order.agent.status = AVAILABLE
    
    // Notify observers (real-time tracking)
    for observer in statusObservers[orderId]:
        observer.onStatusUpdate(order)

DeliveryService.findNearestAgent(location):
    available = [a for a in agents.values() if a.status == AVAILABLE]
    available.sort(by: a → a.currentLocation.distanceTo(location))
    return available[0] if available else null
```

#### Step 5: Extensibility
1. **"What if we add real-time GPS tracking?"** → Agent publishes location updates. `TrackingService` stores latest location per order. Client polls or uses WebSocket.
2. **"What if we add restaurant ratings/reviews?"** → `Review` entity. `ReviewService` with `addReview()`. Aggregate rating computed and cached.
3. **"What if we add promo codes/discounts?"** → Strategy pattern: `DiscountStrategy`. `Order` applies discount before payment. Validate promo code eligibility.

---

### 20. Online Shopping System (Amazon)

**Difficulty:** Hard | **Companies:** Amazon, Flipkart, eBay

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Product catalog with search? A: Yes
2. Q: Shopping cart? A: Yes, persistent cart per user
3. Q: Order with multiple items? A: Yes
4. Q: Payment and shipping? A: Yes, basic flow

##### Final Requirements
1. Product catalog with search by name/category
2. Users can add/remove items from a shopping cart
3. Checkout flow: cart → order → payment → shipping
4. Track order status (placed, shipped, delivered)
5. Product inventory management (prevent overselling)

#### Step 2: Core Entities
- **ProductCatalog** — search and browse
- **Product** — item for sale
- **Cart** — user's shopping cart
- **Order** — placed order
- **User** — customer
- **PaymentService** — processes payments
- **ShippingService** — tracks delivery

#### Step 3: Class Design

```
enum OrderStatus { PLACED, PAID, SHIPPED, DELIVERED, CANCELLED }

class Product:
    id: String
    name: String
    description: String
    price: float
    category: String
    stockQuantity: int

class CartItem:
    product: Product
    quantity: int

class Cart:
    userId: String
    items: Map<String, CartItem>  // productId → CartItem
    
    addItem(product, quantity)
    removeItem(productId)
    updateQuantity(productId, quantity)
    getTotal() → float
    clear()

class OrderItem:
    product: Product
    quantity: int
    priceAtPurchase: float

class Order:
    id: String
    userId: String
    items: List<OrderItem>
    totalPrice: float
    status: OrderStatus
    shippingAddress: Address
    createdAt: DateTime

class ProductCatalog:
    products: Map<String, Product>
    
    search(query: String) → List<Product>
    searchByCategory(category: String) → List<Product>
    getProduct(productId) → Product?

class OrderService:
    orders: Map<String, Order>
    productCatalog: ProductCatalog
    paymentService: PaymentService
    
    checkout(cart: Cart, userId, address) → Order?
    cancelOrder(orderId) → bool
    getOrderStatus(orderId) → OrderStatus
```

#### Step 4: Implementation

```
OrderService.checkout(cart, userId, address):
    if cart.items.isEmpty(): return null
    
    // Validate stock and reserve
    orderItems = []
    for cartItem in cart.items.values():
        product = cartItem.product
        if product.stockQuantity < cartItem.quantity:
            throw InsufficientStock(product)
        orderItems.add(OrderItem(product, cartItem.quantity, product.price))
    
    totalPrice = sum(item.priceAtPurchase * item.quantity for item in orderItems)
    
    // Process payment
    if not paymentService.charge(userId, totalPrice):
        throw PaymentFailed
    
    // Deduct stock
    for item in orderItems:
        item.product.stockQuantity -= item.quantity
    
    order = Order(generateId(), userId, orderItems, totalPrice, PAID, address, now())
    orders[order.id] = order
    cart.clear()
    
    // Trigger shipping
    shippingService.scheduleShipping(order)
    return order

OrderService.cancelOrder(orderId):
    order = orders[orderId]
    if order.status not in [PLACED, PAID]: return false
    
    // Restore stock
    for item in order.items:
        item.product.stockQuantity += item.quantity
    
    // Refund
    paymentService.refund(order.userId, order.totalPrice)
    order.status = CANCELLED
    return true
```

#### Step 5: Extensibility
1. **"What if we add product recommendations?"** → `RecommendationEngine` interface. Implementations: `CollaborativeFiltering`, `ContentBased`. Plugged into product browsing.
2. **"What if we add wishlists?"** → New `Wishlist` entity per user. Similar to Cart but without checkout. `moveToCart()` convenience method.
3. **"What if we add seller/marketplace model?"** → `Seller` entity. Product gets `sellerId`. `OrderItem` tracks seller. Payment splits between sellers.

**Concurrency Note:** Stock deduction in `checkout()` must be atomic. Use optimistic locking (`UPDATE stock SET qty = qty - N WHERE qty >= N`) or pessimistic lock.

---

### 21. Logger System

**Difficulty:** Medium | **Companies:** Amazon, Google

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Log levels? A: DEBUG, INFO, WARN, ERROR, FATAL
2. Q: Multiple output destinations? A: Yes (console, file, remote)
3. Q: Thread-safe? A: Yes
4. Q: Log formatting? A: Configurable format (timestamp, level, message)

##### Final Requirements
1. Support log levels: DEBUG, INFO, WARN, ERROR, FATAL
2. Configurable minimum log level (filter lower-priority messages)
3. Multiple output sinks: Console, File, Remote (extensible)
4. Configurable log format (timestamp + level + message)
5. Thread-safe: multiple threads can log simultaneously
6. Singleton logger instance

#### Step 2: Core Entities
- **Logger** — singleton entry point
- **LogLevel** — severity enum
- **LogMessage** — structured log entry
- **LogSink** — output destination (interface)
- **LogFormatter** — formats log messages

#### Step 3: Class Design

```
enum LogLevel { DEBUG, INFO, WARN, ERROR, FATAL }  // ordered by severity

class LogMessage:
    timestamp: DateTime
    level: LogLevel
    message: String
    threadName: String

interface LogSink:
    write(formattedMessage: String)

class ConsoleSink implements LogSink:
    write(msg) → print to stdout

class FileSink implements LogSink:
    filePath: String
    writer: FileWriter
    write(msg) → append to file

class RemoteSink implements LogSink:
    endpoint: String
    write(msg) → send over network

interface LogFormatter:
    format(logMessage: LogMessage) → String

class DefaultFormatter implements LogFormatter:
    format(msg) → "[{timestamp}] [{level}] [{thread}] {message}"

class Logger:
    private static instance: Logger
    minLevel: LogLevel
    sinks: List<LogSink>
    formatter: LogFormatter
    lock: Mutex
    
    static getInstance() → Logger
    setLevel(level: LogLevel)
    addSink(sink: LogSink)
    
    debug(msg), info(msg), warn(msg), error(msg), fatal(msg)
    private log(level: LogLevel, message: String)
```

#### Step 4: Implementation

```
Logger.log(level, message):
    if level < minLevel: return  // filter
    
    logMessage = LogMessage(now(), level, message, currentThread().name)
    formatted = formatter.format(logMessage)
    
    synchronized(lock):
        for sink in sinks:
            try:
                sink.write(formatted)
            catch Exception:
                // Don't let sink failure crash the application
                // Optionally write to stderr
                stderr.println("Failed to write to sink: " + e.message)

Logger.getInstance():
    if instance is null:
        synchronized(Logger.class):
            if instance is null:  // double-check locking
                instance = Logger()
    return instance
```

#### Step 5: Extensibility
1. **"What if we add async logging?"** → Add `AsyncSink` decorator wrapping any `LogSink`. Enqueues messages to a bounded queue, background thread drains.
2. **"What if we add log rotation?"** → `RotatingFileSink` extends `FileSink`. When file exceeds size limit, rotate (rename + create new file).
3. **"What if we add structured logging (JSON)?"** → New `JsonFormatter` implementing `LogFormatter`. Returns JSON string. No other class changes needed (OCP).

---

### 22. File System (In-Memory)

**Difficulty:** Medium | **Companies:** Google, Amazon, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Directories and files? A: Yes, hierarchical
2. Q: File content? A: Simple string content
3. Q: Operations? A: Create, read, write, delete, list, move
4. Q: Path format? A: Unix-style (e.g., /home/user/file.txt)

##### Final Requirements
1. Hierarchical file system with directories and files
2. Create, read, write, delete files
3. Create, list, delete directories (recursive delete)
4. Move/rename files and directories
5. Path-based navigation (Unix-style absolute paths)

#### Step 2: Core Entities
- **FileSystem** — top-level API
- **FSNode** — abstract base for files and directories
- **File** — leaf node with content
- **Directory** — contains children (files and directories)

#### Step 3: Class Design

```
abstract class FSNode:
    name: String
    parent: Directory?
    createdAt: DateTime
    modifiedAt: DateTime
    
    getPath() → String  // traverse parent chain

class File extends FSNode:
    content: String
    size: int
    
    read() → String
    write(content: String)

class Directory extends FSNode:
    children: Map<String, FSNode>  // name → node
    
    addChild(node: FSNode)
    removeChild(name: String) → FSNode?
    getChild(name: String) → FSNode?
    listChildren() → List<String>

class FileSystem:
    root: Directory  // "/"
    
    createFile(path: String, content: String) → File
    readFile(path: String) → String
    writeFile(path: String, content: String)
    deleteFile(path: String)
    createDirectory(path: String) → Directory
    listDirectory(path: String) → List<String>
    deleteDirectory(path: String)  // recursive
    move(srcPath, destPath)
    
    private resolvePath(path: String) → FSNode?
    private resolveParent(path: String) → (Directory, String)
```

#### Step 4: Implementation

```
FileSystem.resolvePath(path):
    parts = path.split("/").filter(non-empty)
    current = root
    for part in parts:
        if current is not Directory: return null
        current = current.getChild(part)
        if current is null: return null
    return current

FileSystem.createFile(path, content):
    (parentDir, fileName) = resolveParent(path)
    if parentDir.getChild(fileName) != null: throw AlreadyExists
    
    file = File(fileName, content)
    file.parent = parentDir
    parentDir.addChild(file)
    return file

FileSystem.deleteDirectory(path):
    node = resolvePath(path)
    if node is not Directory: throw NotADirectory
    if node == root: throw CannotDeleteRoot
    
    node.parent.removeChild(node.name)
    // All children are garbage collected (or recursively clean up)

FileSystem.move(srcPath, destPath):
    srcNode = resolvePath(srcPath)
    if srcNode is null: throw NotFound
    
    (destParent, destName) = resolveParent(destPath)
    if destParent.getChild(destName) != null: throw AlreadyExists
    
    srcNode.parent.removeChild(srcNode.name)
    srcNode.name = destName
    srcNode.parent = destParent
    destParent.addChild(srcNode)
```

#### Step 5: Extensibility
1. **"What if we add permissions?"** → Add `permissions: Permissions` to FSNode. Check read/write/execute on each operation. `User` context passed to methods.
2. **"What if we add symlinks?"** → New `SymLink extends FSNode` with `target: String`. `resolvePath` follows symlinks, with cycle detection.
3. **"What if we add search (find by name/content)?"** → Recursive search from directory. `FileSystem.find(dir, pattern)` uses DFS through children.

---

### 23. Stack Overflow

**Difficulty:** Hard | **Companies:** Amazon, Google, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: Core features? A: Questions, answers, voting, comments
2. Q: User reputation? A: Yes, earn rep from upvotes
3. Q: Accept answer? A: Yes, question author marks best answer
4. Q: Tags/search? A: Yes

##### Final Requirements
1. Users can post questions with title, body, and tags
2. Users can post answers to questions
3. Users can upvote/downvote questions and answers
4. Question author can accept one answer
5. User reputation: +10 for upvote received, -2 for downvote received
6. Search questions by keyword or tag

#### Step 2: Core Entities
- **Platform** — top-level service
- **User** — posts, votes, earns reputation
- **Question** — has title, body, tags, answers
- **Answer** — response to a question
- **Comment** — on questions or answers
- **Vote** — upvote or downvote

#### Step 3: Class Design

```
enum VoteType { UPVOTE, DOWNVOTE }

class User:
    id: String
    name: String
    reputation: int
    
    addReputation(points: int)

class Comment:
    id: String
    author: User
    body: String
    createdAt: DateTime

interface Voteable:
    getVotes() → Map<String, VoteType>
    getScore() → int

class Question implements Voteable:
    id: String
    title: String
    body: String
    author: User
    tags: List<String>
    answers: List<Answer>
    comments: List<Comment>
    votes: Map<String, VoteType>   // userId → vote
    acceptedAnswer: Answer?
    createdAt: DateTime
    
    addAnswer(answer: Answer)
    acceptAnswer(answerId: String)
    getScore() → int

class Answer implements Voteable:
    id: String
    body: String
    author: User
    question: Question
    comments: List<Comment>
    votes: Map<String, VoteType>
    isAccepted: bool
    createdAt: DateTime
    
    getScore() → int

class Platform:
    users: Map<String, User>
    questions: Map<String, Question>
    tagIndex: Map<String, List<Question>>  // tag → questions
    
    postQuestion(userId, title, body, tags) → Question
    postAnswer(userId, questionId, body) → Answer
    vote(userId, targetId, voteType)
    acceptAnswer(userId, questionId, answerId)
    search(query: String) → List<Question>
    searchByTag(tag: String) → List<Question>
    addComment(userId, targetId, body) → Comment
```

#### Step 4: Implementation

```
Platform.postQuestion(userId, title, body, tags):
    user = users[userId]
    question = Question(generateId(), title, body, user, tags)
    questions[question.id] = question
    for tag in tags:
        tagIndex[tag].add(question)
    return question

Platform.vote(userId, targetId, voteType):
    target = findVoteable(targetId)  // Question or Answer
    
    // One vote per user per target
    if userId in target.votes:
        oldVote = target.votes[userId]
        reverseReputation(target.author, oldVote)
    
    target.votes[userId] = voteType
    
    if voteType == UPVOTE:
        target.author.addReputation(+10)
    else:
        target.author.addReputation(-2)

Platform.acceptAnswer(userId, questionId, answerId):
    question = questions[questionId]
    if question.author.id != userId: throw Unauthorized
    
    answer = question.answers.find(a → a.id == answerId)
    if question.acceptedAnswer != null:
        question.acceptedAnswer.isAccepted = false
    answer.isAccepted = true
    question.acceptedAnswer = answer
    answer.author.addReputation(+15)

Question.getScore():
    return sum(1 if v == UPVOTE else -1 for v in votes.values())
```

#### Step 5: Extensibility
1. **"What if we add bounties?"** → `Bounty` entity attached to Question. User spends reputation. Bounty awarded to answer author manually or auto (highest voted).
2. **"What if we add badges?"** → `Badge` entity. `BadgeService` checks criteria (e.g., first answer, 100 upvotes) and awards badges to users.
3. **"What if we add duplicate question detection?"** → Mark question as duplicate with link to original. `DuplicateDetector` uses title similarity + shared tags.

---

### 24. ATM System

**Difficulty:** Easy-Medium | **Companies:** Amazon, Microsoft

#### Step 1: Requirements

##### Clarifying Questions
1. Q: What operations? A: Check balance, withdraw, deposit
2. Q: Authentication? A: Card + PIN
3. Q: Multiple account types? A: Savings and Checking
4. Q: Cash denominations? A: Yes, dispense in specific denominations

##### Final Requirements
1. User inserts card, enters PIN to authenticate
2. Operations: check balance, withdraw cash, deposit cash
3. ATM has limited cash in specific denominations
4. Withdrawal must handle denomination availability
5. Session ends after operation or timeout

#### Step 2: Core Entities
- **ATM** — physical machine
- **CashDispenser** — manages denominations
- **Card** — bank card with account info
- **Account** — user's bank account
- **BankService** — validates PIN, manages accounts
- **Session** — current user interaction

#### Step 3: Class Design

```
enum TransactionType { WITHDRAWAL, DEPOSIT, BALANCE_CHECK }
enum ATMState { IDLE, AUTHENTICATING, ACTIVE, DISPENSING }

class Card:
    cardNumber: String
    accountId: String

class Account:
    id: String
    balance: float
    
    debit(amount) → bool
    credit(amount)

class CashDispenser:
    denominations: Map<int, int>  // denomination → count (e.g., {100: 50, 50: 100, 20: 200})
    
    canDispense(amount) → bool
    dispense(amount) → Map<int, int>  // denomination → count to dispense

class BankService:
    accounts: Map<String, Account>
    
    authenticate(cardNumber, pin) → Account?
    getBalance(accountId) → float
    withdraw(accountId, amount) → bool
    deposit(accountId, amount)

class ATM:
    cashDispenser: CashDispenser
    bankService: BankService
    state: ATMState
    currentSession: Session?
    
    insertCard(card: Card)
    enterPIN(pin: String) → bool
    checkBalance() → float
    withdraw(amount) → Map<int, int>?
    deposit(amount)
    ejectCard()

class Session:
    account: Account
    card: Card
    startedAt: DateTime
```

#### Step 4: Implementation

```
ATM.insertCard(card):
    if state != IDLE: return
    state = AUTHENTICATING
    currentSession = Session(card)

ATM.enterPIN(pin):
    account = bankService.authenticate(currentSession.card.cardNumber, pin)
    if account is null:
        ejectCard()
        return false
    currentSession.account = account
    state = ACTIVE
    return true

ATM.withdraw(amount):
    if state != ACTIVE: return null
    
    if not cashDispenser.canDispense(amount):
        return null  // ATM can't dispense this amount
    
    if not bankService.withdraw(currentSession.account.id, amount):
        return null  // insufficient funds
    
    bills = cashDispenser.dispense(amount)
    return bills

CashDispenser.dispense(amount):
    result = {}
    remaining = amount
    for denom in sorted(denominations.keys(), descending):
        count = min(remaining / denom, denominations[denom])
        if count > 0:
            result[denom] = count
            remaining -= denom * count
            denominations[denom] -= count
    if remaining > 0:
        // Rollback — can't make exact change
        // Restore denominations
        return null
    return result
```

#### Step 5: Extensibility
1. **"What if we add transfer between accounts?"** → New `transfer(fromAccount, toAccount, amount)` operation on `BankService`. ATM prompts for target account.
2. **"What if we add receipt printing?"** → `ReceiptPrinter` component. `ATM` calls `printer.print(transaction)` after each operation.
3. **"What if we add multiple card types?"** → Strategy pattern for card authentication: `PINAuth`, `ChipAuth`, `ContactlessAuth`.

---

## Summary: Problem Difficulty Matrix

| # | Problem | Difficulty | Key Patterns | Key Concepts |
|---|---------|-----------|-------------|-------------|
| 1 | Tic-Tac-Toe | Easy | — | Basic OOP, enum, game loop |
| 2 | Connect Four | Easy | — | Grid traversal, gravity mechanic |
| 3 | Amazon Locker | Easy | — | Resource allocation, access codes |
| 4 | Vending Machine | Easy | State Machine, Strategy | State transitions, payment |
| 5 | Snake and Ladder | Easy | — | Dice, board effects, game loop |
| 6 | Parking Lot | Medium | Strategy, Factory | Multi-level, spot assignment, concurrency |
| 7 | Elevator System | Medium | Strategy, State Machine | SCAN algorithm, multi-elevator dispatch |
| 8 | Library Management | Medium | Observer | Reservations, loans, late fees |
| 9 | Hotel Booking | Medium | Strategy | Date ranges, overlap detection, cancellation |
| 10 | Movie Ticket Booking | Medium | — | Seat selection, temporary holds, concurrency |
| 11 | LRU Cache | Medium | — | HashMap + DoublyLinkedList, O(1) ops |
| 12 | Rate Limiter | Medium | Strategy | Token bucket, sliding window, concurrency |
| 13 | Splitwise | Medium | — | Balance graph, debt simplification |
| 14 | Chess | Medium | Factory, Strategy | Polymorphism (pieces), check detection |
| 15 | Inventory Management | Medium | Observer | Multi-warehouse, concurrency, alerts |
| 16 | Task Scheduler | Medium | Strategy | Priority queue, retry, recurring tasks |
| 17 | Pub/Sub Messaging | Medium | Observer | Topics, fan-out, async delivery |
| 18 | Ride Sharing | Hard | Strategy, Observer | Matching, surge pricing, real-time |
| 19 | Food Delivery | Hard | Observer, Strategy | Multi-party (customer/restaurant/agent) |
| 20 | Online Shopping | Hard | Strategy, Facade | Cart → Order → Payment → Shipping pipeline |
| 21 | Logger System | Medium | Singleton, Strategy | Multiple sinks, thread safety, formatting |
| 22 | File System | Medium | Composite | Tree traversal, path resolution |
| 23 | Stack Overflow | Hard | Observer | Voting, reputation, search |
| 24 | ATM System | Easy-Med | State Machine | Cash dispensing, authentication |

---

## Study Order (Recommended)

### Phase 1: Foundations (Week 1)
- [ ] Read: Introduction to LLD
- [ ] Read: Delivery Framework — practice with Tic-Tac-Toe
- [ ] Read: Design Principles (KISS, DRY, YAGNI, SOLID)
- [ ] Read: OOP Concepts
- [ ] Read: Design Patterns
- [ ] Read: Concurrency basics

### Phase 2: Easy Problems (Week 2)
- [ ] Solve: Tic-Tac-Toe
- [ ] Solve: Connect Four
- [ ] Solve: Amazon Locker
- [ ] Solve: Vending Machine
- [ ] Solve: Snake and Ladder

### Phase 3: Core Medium Problems (Weeks 3–4)
- [ ] Solve: Parking Lot ⭐
- [ ] Solve: Elevator System ⭐
- [ ] Solve: Library Management
- [ ] Solve: LRU Cache ⭐
- [ ] Solve: Rate Limiter ⭐
- [ ] Solve: Logger System

### Phase 4: Advanced Medium Problems (Weeks 5–6)
- [ ] Solve: Hotel Booking
- [ ] Solve: Movie Ticket Booking
- [ ] Solve: Chess
- [ ] Solve: Splitwise
- [ ] Solve: Inventory Management
- [ ] Solve: Task Scheduler
- [ ] Solve: Pub/Sub Messaging
- [ ] Solve: File System
- [ ] Solve: ATM System

### Phase 5: Hard Problems (Weeks 7–8)
- [ ] Solve: Ride Sharing (Uber)
- [ ] Solve: Food Delivery (Zomato)
- [ ] Solve: Online Shopping (Amazon)
- [ ] Solve: Stack Overflow

### Phase 6: Review & Mock Interviews
- [ ] Re-solve starred (⭐) problems under timed conditions (45 min)
- [ ] Practice explaining your design out loud
- [ ] Focus on extensibility discussion — this separates mid from senior

---

## Quick Reference: Pattern Selection Guide

| Scenario | Pattern |
|----------|---------|
| Need different algorithms swappable at runtime | **Strategy** |
| Object goes through well-defined states | **State Machine** |
| Create objects without specifying exact class | **Factory** |
| Complex object construction with many params | **Builder** |
| Need global single instance | **Singleton** |
| Add behavior dynamically without subclassing | **Decorator** |
| Simplify complex subsystem API | **Facade** |
| Notify multiple objects of state changes | **Observer** |
| Treat individual objects and groups uniformly | **Composite** |
| Save and restore object state | **Memento** |
| Encapsulate operations for undo/redo | **Command** |
