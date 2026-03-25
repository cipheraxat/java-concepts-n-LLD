# Elevator System

**Difficulty:** Medium | **Companies:** Amazon, Google, Microsoft

---

## Requirements

> "Design the object-oriented backend for an elevator system in a building. The system manages multiple elevators, handles hall-button requests and in-cabin floor selections, and moves elevators efficiently using the SCAN algorithm."

### Clarifying Questions

> **You:** "How many elevators and floors?"
>
> **Interviewer:** "Configurable — say 3 elevators and 20 floors."

> **You:** "How does scheduling work? Simple nearest-elevator or something smarter?"
>
> **Interviewer:** "Use the SCAN (sweep) algorithm — serve all stops in one direction before reversing. Dispatch the best elevator based on a cost heuristic."

> **You:** "What types of requests exist?"
>
> **Interviewer:** "External requests (hall button: floor + direction) and internal requests (cabin floor button pressed by a passenger inside)."

> **You:** "Do we need weight limits or emergency stop?"
>
> **Interviewer:** "No. Those are extensibility points."

### Final Requirements

```
Requirements:
1. Configurable number of elevators and floors
2. SCAN algorithm: serve all stops in current direction, then reverse
3. External requests: hall button specifies floor and desired direction
4. Internal requests: passenger presses a floor button inside the cabin
5. Dispatcher assigns external requests to the best elevator using a cost heuristic

Out of scope:
- Weight limits
- Emergency stop
- Door open/close mechanics
- Priority floors (e.g., lobby)
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **ElevatorSystem** | Top-level facade. Separates external requests (via Dispatcher) from internal requests (direct to elevator). Advances the simulation via `tick()`. |
| **Elevator** | Independent state machine. Owns pending stops via two TreeSets. Moves one floor per tick, opens doors at stops, reverses when sweep is exhausted. |
| **ExternalRequest** | Immutable value object for a hall button press — floor + direction. |
| **Dispatcher** | Greedy min-cost heuristic. Selects the elevator with the lowest estimated cost to serve a request. |

---

## Class Design

### Elevator

The Elevator is the core — it implements the SCAN algorithm using two TreeSets for efficient stop management.

| Requirement | What Elevator must track |
|-------------|--------------------------|
| "Serve stops in current direction" | Two TreeSets: upStops (ascending) and downStops (descending) |
| "Move floor by floor" | Current floor and direction |
| "Reverse when sweep exhausted" | Direction enum (UP, DOWN, IDLE) |

```
class Elevator:
    - id: int
    - currentFloor: int
    - direction: Direction
    - upStops: TreeSet<Integer>          // natural order
    - downStops: TreeSet<Integer>        // reverse order

    + addStop(floor) → void
    + step() → void
    + estimatedCost(requestFloor, requestDir) → int
    + isIdle() → boolean
```

### Dispatcher

| Requirement | What Dispatcher must track |
|-------------|---------------------------|
| "Assign to best elevator" | Access to all elevators for comparison |

```
class Dispatcher:
    - elevators: List<Elevator>

    + dispatch(request) → Elevator
```

### ElevatorSystem

```
class ElevatorSystem:
    - elevators: List<Elevator>
    - dispatcher: Dispatcher

    + externalRequest(floor, direction) → void
    + internalRequest(elevatorId, floor) → void
    + tick() → void
    + runUntilIdle() → void
```

### Final Class Design

```
class ElevatorSystem:
    - elevators: List<Elevator>
    - dispatcher: Dispatcher

    + externalRequest(floor, direction) → void
    + internalRequest(elevatorId, floor) → void
    + tick() → void
    + runUntilIdle() → void

class Elevator:
    - id: int
    - currentFloor: int
    - direction: Direction
    - upStops: TreeSet<Integer>
    - downStops: TreeSet<Integer>

    + addStop(floor) → void
    + step() → void
    + estimatedCost(floor, direction) → int
    + isIdle() → boolean

class Dispatcher:
    - elevators: List<Elevator>

    + dispatch(request) → Elevator

class ExternalRequest:
    - floor: int
    - direction: Direction

enum Direction:
    UP, DOWN, IDLE
```

---

## Implementation

### Elevator.addStop

**Core logic:** Route the floor to the correct TreeSet based on whether it's above or below the current floor, and the current direction.

```
addStop(floor)
    if floor == currentFloor → return
    if floor > currentFloor → upStops.add(floor)
    else → downStops.add(floor)
    if direction == IDLE:
        direction = floor > currentFloor ? UP : DOWN
```

### Elevator.step

**Core logic:** Move one floor toward the next stop. Open doors if at a stop. Reverse direction when the current sweep is exhausted.

```
step()
    if direction == IDLE → return

    if direction == UP:
        currentFloor++
        if upStops.contains(currentFloor):
            upStops.remove(currentFloor)
            print "Elevator {id} opened at floor {currentFloor}"
        if upStops.isEmpty():
            if downStops.isEmpty() → direction = IDLE
            else → direction = DOWN

    if direction == DOWN:
        currentFloor--
        if downStops.contains(currentFloor):
            downStops.remove(currentFloor)
            print "Elevator {id} opened at floor {currentFloor}"
        if downStops.isEmpty():
            if upStops.isEmpty() → direction = IDLE
            else → direction = UP
```

### Elevator.estimatedCost

**Core logic:** Heuristic for dispatcher ranking. Three cases: idle (absolute distance), same direction and on the way (direct distance), otherwise penalized.

```
estimatedCost(requestFloor, requestDir)
    if direction == IDLE:
        return abs(currentFloor - requestFloor)

    if direction == requestDir:
        if direction == UP and requestFloor >= currentFloor:
            return requestFloor - currentFloor
        if direction == DOWN and requestFloor <= currentFloor:
            return currentFloor - requestFloor

    // Wrong direction or passed the floor — full sweep penalty
    return totalFloors * 2
```

### Complete Code Implementation

```java
public enum Direction { UP, DOWN, IDLE }
```

```java
public class ExternalRequest {
    private final int floor;
    private final Direction direction;

    public ExternalRequest(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    public int getFloor()          { return floor; }
    public Direction getDirection() { return direction; }
}
```

```java
import java.util.Collections;
import java.util.TreeSet;

public class Elevator {
    private final int id;
    private final int totalFloors;
    private int currentFloor;
    private Direction direction;
    private final TreeSet<Integer> upStops;
    private final TreeSet<Integer> downStops;

    public Elevator(int id, int totalFloors) {
        this.id = id;
        this.totalFloors = totalFloors;
        this.currentFloor = 1;
        this.direction = Direction.IDLE;
        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>(Collections.reverseOrder());
    }

    public void addStop(int floor) {
        if (floor == currentFloor) return;
        if (floor > currentFloor) upStops.add(floor);
        else downStops.add(floor);

        if (direction == Direction.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        }
    }

    public void step() {
        if (direction == Direction.IDLE) return;

        if (direction == Direction.UP) {
            currentFloor++;
            if (upStops.remove(currentFloor)) {
                System.out.printf("Elevator %d opened at floor %d%n", id, currentFloor);
            }
            if (upStops.isEmpty()) {
                direction = downStops.isEmpty() ? Direction.IDLE : Direction.DOWN;
            }
        } else {
            currentFloor--;
            if (downStops.remove(currentFloor)) {
                System.out.printf("Elevator %d opened at floor %d%n", id, currentFloor);
            }
            if (downStops.isEmpty()) {
                direction = upStops.isEmpty() ? Direction.IDLE : Direction.UP;
            }
        }
    }

    public int estimatedCost(int requestFloor, Direction requestDir) {
        if (direction == Direction.IDLE) {
            return Math.abs(currentFloor - requestFloor);
        }
        if (direction == requestDir) {
            if (direction == Direction.UP && requestFloor >= currentFloor)
                return requestFloor - currentFloor;
            if (direction == Direction.DOWN && requestFloor <= currentFloor)
                return currentFloor - requestFloor;
        }
        return totalFloors * 2;
    }

    public boolean isIdle()       { return direction == Direction.IDLE; }
    public int getId()            { return id; }
    public int getCurrentFloor()  { return currentFloor; }
    public Direction getDirection() { return direction; }
}
```

```java
import java.util.Comparator;
import java.util.List;

public class Dispatcher {
    private final List<Elevator> elevators;

    public Dispatcher(List<Elevator> elevators) {
        this.elevators = elevators;
    }

    public Elevator dispatch(ExternalRequest request) {
        return elevators.stream()
            .min(Comparator.comparingInt(
                e -> e.estimatedCost(request.getFloor(), request.getDirection())))
            .orElseThrow();
    }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class ElevatorSystem {
    private final List<Elevator> elevators;
    private final Dispatcher dispatcher;

    public ElevatorSystem(int numElevators, int totalFloors) {
        elevators = new ArrayList<>();
        for (int i = 1; i <= numElevators; i++) {
            elevators.add(new Elevator(i, totalFloors));
        }
        dispatcher = new Dispatcher(elevators);
    }

    public void externalRequest(int floor, Direction direction) {
        Elevator best = dispatcher.dispatch(new ExternalRequest(floor, direction));
        best.addStop(floor);
        System.out.printf("Assigned floor %d (%s) to Elevator %d%n",
            floor, direction, best.getId());
    }

    public void internalRequest(int elevatorId, int floor) {
        elevators.stream()
            .filter(e -> e.getId() == elevatorId)
            .findFirst()
            .ifPresent(e -> e.addStop(floor));
    }

    public void tick() {
        elevators.forEach(Elevator::step);
    }

    public void runUntilIdle() {
        int maxTicks = 200;
        while (maxTicks-- > 0 && elevators.stream().anyMatch(e -> !e.isIdle())) {
            tick();
        }
    }
}
```

### Verification

```
Setup: 2 elevators, 10 floors. Both start at floor 1, IDLE.

Step 1: externalRequest(5, UP)
  Elevator 1 cost: |1-5| = 4 (idle). Elevator 2 cost: |1-5| = 4 (idle).
  Tie → Elevator 1 selected. addStop(5) → upStops={5}, direction=UP

Step 2: externalRequest(3, UP)
  Elevator 1 cost: direction=UP, 3>=1 → 3-1=2
  Elevator 2 cost: |1-3| = 2 (idle). Tie → Elevator 1.
  addStop(3) → upStops={3, 5}

Step 3: tick() (x2)
  Tick 1: Elevator 1 moves 1→2 (no stop)
  Tick 2: Elevator 1 moves 2→3 → upStops has 3 → open doors. upStops={5}

Step 4: internalRequest(1, 7)
  Elevator 1 addStop(7) → upStops={5, 7}

Step 5: tick() (x2)
  Tick 3: 3→4, Tick 4: 4→5 → open doors. upStops={7}

Step 6: tick() (x2)
  Tick 5: 5→6, Tick 6: 6→7 → open doors. upStops empty, downStops empty → IDLE
```

---

## Extensibility

### 1. "How would you add emergency priority?"

> "I'd add a `RequestPriority` enum (NORMAL, EMERGENCY). An emergency request clears all existing stops and sends the elevator directly to the emergency floor. The dispatcher would select the nearest elevator regardless of cost heuristic."

```
enum RequestPriority:
    NORMAL, EMERGENCY

// In Elevator:
handleEmergency(floor)
    upStops.clear()
    downStops.clear()
    addStop(floor)
```

### 2. "What about capacity tracking?"

> "I'd add `maxCapacity` and `currentOccupancy` to Elevator. A `canBoard()` check would be called before adding passengers. The dispatcher could also factor capacity into cost — a nearly full elevator would have higher cost."

```
class Elevator:
    - maxCapacity: int
    - currentOccupancy: int

    + canBoard() → boolean
        return currentOccupancy < maxCapacity

// In dispatcher cost: if !elevator.canBoard() return MAX_VALUE
```

### 3. "How would you add a monitoring dashboard?"

> "I'd use the Observer pattern. Define an `ElevatorObserver` interface with callbacks like `onFloorReached(elevatorId, floor)` and `onDoorsOpened(elevatorId, floor)`. The Elevator notifies observers during `step()`. A dashboard observer can update a UI, while a logging observer can write to a file."

```
interface ElevatorObserver:
    + onFloorReached(elevatorId, floor)
    + onDoorsOpened(elevatorId, floor)

// In Elevator.step():
observers.forEach(o -> o.onFloorReached(id, currentFloor))
```

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can model a single elevator with a list of stops and move it floor by floor. A simple loop that visits stops in order is fine. You should handle both going up and going down. Multi-elevator support and dispatching aren't expected. If your elevator correctly visits requested floors and stops at them, you're doing well.

### Mid-level

For mid-level candidates, I expect multiple elevators with a dispatcher. The SCAN algorithm should be implemented with TreeSets for efficient next-stop lookup. The dispatcher should at least choose the nearest idle elevator. You should separate external requests (hall button) from internal requests (cabin button). I'd expect you to discuss the cost heuristic and explain why TreeSet (O(log n) insert, O(1) first) is better than a sorted list.

### Senior

Senior candidates should produce a cost heuristic that handles in-motion elevators — not just idle distance but same-direction-on-the-way vs. wrong-direction penalties. You'd explain why two TreeSets (upStops and downStops) are used instead of one, and discuss the safety limit on `runUntilIdle`. Extensibility discussions should cover emergency priority, capacity tracking, and the Observer pattern for monitoring. You might also discuss fairness (preventing starvation of distant floors).