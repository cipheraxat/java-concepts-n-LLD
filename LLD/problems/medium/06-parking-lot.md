# Parking Lot

**Difficulty:** Medium | **Companies:** Amazon, Google, Microsoft, Meta

---

## Requirements

As the interview begins, we'll likely be greeted by a simple prompt to set the stage for the architecture we need to design.

> "Design the object-oriented backend for a multi-level parking lot. Vehicles enter, are assigned the nearest available spot, receive a ticket, park, and pay an hourly fee on exit."

Before jumping into class design, you should ask questions of your interviewer. The goal here is to turn that vague prompt into a concrete specification — something you can actually build against.

### Clarifying Questions

> **You:** "How many levels does the lot have, and are there different spot sizes?"
>
> **Interviewer:** "Multiple levels. Three spot sizes — Small, Medium, Large — and three vehicle types — Motorcycle, Car, Truck."

> **You:** "How does vehicle-to-spot matching work?"
>
> **Interviewer:** "Motorcycle fits any spot. Car fits Medium or Large. Truck fits Large only."

Good. Now think about the fee structure.

> **You:** "How is the fee calculated?"
>
> **Interviewer:** "Hourly rate based on vehicle type. Ceiling of hours — any partial hour counts as full."

> **You:** "Do we need to handle concurrent entry and exit?"
>
> **Interviewer:** "Yes. Multiple vehicles can enter and exit simultaneously. Thread safety matters."

> **You:** "Do we need features like monthly passes or EV charging?"
>
> **Interviewer:** "No. Keep those as extensibility discussion points."

### Final Requirements

```
Requirements:
1. Multi-level lot with spots of sizes Small, Medium, Large
2. Vehicles: Motorcycle (fits any), Car (fits Medium/Large), Truck (fits Large only)
3. On entry: assign the nearest available spot, issue a ticket with timestamp
4. On exit: calculate fee based on vehicle type and ceiling hours parked
5. Thread-safe: multiple entries and exits concurrently

Out of scope:
- Monthly passes
- EV charging spots
- Dynamic pricing
- Reservation system
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **ParkingLot** | Top-level facade. Hides multi-level layout. Exposes `park(vehicle)` and `unpark(licensePlate)`. Maintains active tickets. |
| **Level** | A floor with rows of spots. Independently tracks availability. Enables per-level filtering. |
| **Spot** | Smallest lockable unit. Has a size and occupancy state. `ReentrantLock` per spot prevents double-parking. |
| **Vehicle** | Abstract base with license plate and type. Subclasses (Motorcycle, Car, Truck) define `requiredSize`. |
| **Ticket** | Links a vehicle to a spot and records entry time. Decoupled from Spot so spot can be reused after exit. |
| **FeeCalculator** | Strategy interface for fee computation. Allows swapping hourly, flat-rate, or dynamic pricing. |

---

## Class Design

### ParkingLot

| Requirement | What ParkingLot must track |
|-------------|---------------------------|
| "Multi-level lot" | List of Level objects |
| "Issue a ticket" | Map of licensePlate → Ticket for active parkers |
| "Calculate fee on exit" | A FeeCalculator strategy |

```
class ParkingLot:
    - levels: List<Level>
    - activeTickets: Map<String, Ticket>
    - feeCalculator: FeeCalculator
    - ticketCounter: AtomicLong

    + park(vehicle) → Optional<Ticket>
    + unpark(licensePlate) → Optional<Double>
```

### Level

| Requirement | What Level must track |
|-------------|----------------------|
| "Rows of spots" | A list of Spots |
| "Find nearest available" | Stream-based search filtered by size compatibility |

```
class Level:
    - levelId: int
    - spots: List<Spot>

    + findAndPark(vehicle) → Optional<Spot>
    + releaseSpot(spotId) → void
```

### Spot

| Requirement | What Spot must track |
|-------------|---------------------|
| "Size and occupancy" | Size enum, occupied flag, current vehicle |
| "Thread-safe assignment" | ReentrantLock for atomic check-and-park |

```
class Spot:
    - spotId: String
    - size: SpotSize
    - occupied: boolean
    - currentVehicle: Vehicle?
    - lock: ReentrantLock

    + tryPark(vehicle) → boolean
    + release() → void
    + canFit(vehicle) → boolean
```

### Vehicle

```
abstract class Vehicle:
    - licensePlate: String
    - type: VehicleType

class Motorcycle extends Vehicle
class Car extends Vehicle
class Truck extends Vehicle

enum VehicleType:
    MOTORCYCLE, CAR, TRUCK
```

### FeeCalculator (Strategy)

```
interface FeeCalculator:
    + calculateFee(ticket, exitTime) → double

class HourlyFeeCalculator implements FeeCalculator
```

### Final Class Design

```
class ParkingLot:
    - levels: List<Level>
    - activeTickets: Map<String, Ticket>
    - feeCalculator: FeeCalculator
    - ticketCounter: AtomicLong

    + park(vehicle) → Optional<Ticket>
    + unpark(licensePlate) → Optional<Double>

class Level:
    - levelId: int
    - spots: List<Spot>

    + findAndPark(vehicle) → Optional<Spot>
    + releaseSpot(spotId) → void

class Spot:
    - spotId: String
    - size: SpotSize
    - occupied: boolean
    - currentVehicle: Vehicle?
    - lock: ReentrantLock

    + tryPark(vehicle) → boolean
    + release() → void
    + canFit(vehicle) → boolean

class Ticket:
    - ticketId: String
    - vehicle: Vehicle
    - spot: Spot
    - level: Level
    - entryTime: LocalDateTime

abstract class Vehicle:
    - licensePlate: String
    - type: VehicleType

interface FeeCalculator:
    + calculateFee(ticket, exitTime) → double

class HourlyFeeCalculator implements FeeCalculator

enum SpotSize: SMALL, MEDIUM, LARGE
enum VehicleType: MOTORCYCLE, CAR, TRUCK
```

---

## Implementation

### Spot.tryPark

**Core logic:** Atomic check-and-park using ReentrantLock. Returns true only if the spot is compatible, available, and the lock is acquired.

```
tryPark(vehicle)
    if !canFit(vehicle) → return false
    if !lock.tryLock() → return false
    try:
        if occupied → return false
        occupied = true
        currentVehicle = vehicle
        return true
    finally:
        lock.unlock()
```

### Level.findAndPark

**Core logic:** Stream through spots, filter by compatibility, try to park on each until one succeeds.

```
findAndPark(vehicle)
    for each spot in spots:
        if spot.canFit(vehicle) and spot.tryPark(vehicle)
            return spot
    return null
```

### ParkingLot.park

**Core logic:** Try each level in order. If a spot is found, create a Ticket and store it.

**Edge cases:** Vehicle already parked (duplicate license plate). No spot available.

```
park(vehicle)
    if activeTickets.contains(vehicle.licensePlate) → return empty
    for each level:
        spot = level.findAndPark(vehicle)
        if spot != null:
            ticket = new Ticket(nextId(), vehicle, spot, level, now())
            activeTickets[vehicle.licensePlate] = ticket
            return ticket
    return empty
```

### ParkingLot.unpark

**Core logic:** Look up the ticket, calculate the fee, release the spot, remove the ticket.

```
unpark(licensePlate)
    ticket = activeTickets.remove(licensePlate)
    if ticket is null → return empty
    fee = feeCalculator.calculateFee(ticket, now())
    ticket.level.releaseSpot(ticket.spot.spotId)
    return fee
```

### HourlyFeeCalculator

**Core logic:** Ceiling hours × rate based on vehicle type.

```
calculateFee(ticket, exitTime)
    hours = ceilHours(ticket.entryTime, exitTime)
    rate = switch(ticket.vehicle.type):
        MOTORCYCLE → 1.0
        CAR → 2.0
        TRUCK → 3.0
    return hours × rate
```

### Complete Code Implementation

```java
public enum SpotSize { SMALL, MEDIUM, LARGE }

public enum VehicleType { MOTORCYCLE, CAR, TRUCK }
```

```java
public abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    protected Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public String getLicensePlate() { return licensePlate; }
    public VehicleType getType()    { return type; }
}

public class Motorcycle extends Vehicle {
    public Motorcycle(String plate) { super(plate, VehicleType.MOTORCYCLE); }
}
public class Car extends Vehicle {
    public Car(String plate) { super(plate, VehicleType.CAR); }
}
public class Truck extends Vehicle {
    public Truck(String plate) { super(plate, VehicleType.TRUCK); }
}
```

```java
import java.util.concurrent.locks.ReentrantLock;

public class Spot {
    private final String spotId;
    private final SpotSize size;
    private volatile boolean occupied;
    private Vehicle currentVehicle;
    private final ReentrantLock lock = new ReentrantLock();

    public Spot(String spotId, SpotSize size) {
        this.spotId = spotId;
        this.size = size;
    }

    public boolean canFit(Vehicle vehicle) {
        return switch (vehicle.getType()) {
            case MOTORCYCLE -> true;
            case CAR -> size == SpotSize.MEDIUM || size == SpotSize.LARGE;
            case TRUCK -> size == SpotSize.LARGE;
        };
    }

    public boolean tryPark(Vehicle vehicle) {
        if (!canFit(vehicle)) return false;
        if (!lock.tryLock()) return false;
        try {
            if (occupied) return false;
            occupied = true;
            currentVehicle = vehicle;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void release() {
        lock.lock();
        try {
            occupied = false;
            currentVehicle = null;
        } finally {
            lock.unlock();
        }
    }

    public String getSpotId()       { return spotId; }
    public SpotSize getSize()       { return size; }
    public boolean isOccupied()     { return occupied; }
}
```

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Level {
    private final int levelId;
    private final List<Spot> spots;

    public Level(int levelId, List<Spot> spots) {
        this.levelId = levelId;
        this.spots = new ArrayList<>(spots);
    }

    public Optional<Spot> findAndPark(Vehicle vehicle) {
        return spots.stream()
            .filter(s -> !s.isOccupied() && s.canFit(vehicle))
            .filter(s -> s.tryPark(vehicle))
            .findFirst();
    }

    public void releaseSpot(String spotId) {
        spots.stream()
            .filter(s -> s.getSpotId().equals(spotId))
            .findFirst()
            .ifPresent(Spot::release);
    }

    public int getLevelId() { return levelId; }
}
```

```java
import java.time.LocalDateTime;

public class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final Spot spot;
    private final Level level;
    private final LocalDateTime entryTime;

    public Ticket(String ticketId, Vehicle vehicle, Spot spot,
                  Level level, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.level = level;
        this.entryTime = entryTime;
    }

    public String getTicketId()       { return ticketId; }
    public Vehicle getVehicle()       { return vehicle; }
    public Spot getSpot()             { return spot; }
    public Level getLevel()           { return level; }
    public LocalDateTime getEntryTime() { return entryTime; }
}
```

```java
import java.time.LocalDateTime;

public interface FeeCalculator {
    double calculateFee(Ticket ticket, LocalDateTime exitTime);
}
```

```java
import java.time.Duration;
import java.time.LocalDateTime;

public class HourlyFeeCalculator implements FeeCalculator {
    @Override
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        long minutes = Duration.between(ticket.getEntryTime(), exitTime).toMinutes();
        long hours = (minutes + 59) / 60; // ceiling
        double rate = switch (ticket.getVehicle().getType()) {
            case MOTORCYCLE -> 1.0;
            case CAR -> 2.0;
            case TRUCK -> 3.0;
        };
        return hours * rate;
    }
}
```

```java
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ParkingLot {
    private final List<Level> levels;
    private final Map<String, Ticket> activeTickets = new HashMap<>();
    private final FeeCalculator feeCalculator;
    private final AtomicLong ticketCounter = new AtomicLong(1);

    public ParkingLot(List<Level> levels, FeeCalculator feeCalculator) {
        this.levels = levels;
        this.feeCalculator = feeCalculator;
    }

    public synchronized Optional<Ticket> park(Vehicle vehicle) {
        if (activeTickets.containsKey(vehicle.getLicensePlate())) {
            return Optional.empty();
        }
        for (Level level : levels) {
            Optional<Spot> spot = level.findAndPark(vehicle);
            if (spot.isPresent()) {
                Ticket ticket = new Ticket(
                    "T-" + ticketCounter.getAndIncrement(),
                    vehicle, spot.get(), level, LocalDateTime.now()
                );
                activeTickets.put(vehicle.getLicensePlate(), ticket);
                return Optional.of(ticket);
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<Double> unpark(String licensePlate) {
        Ticket ticket = activeTickets.remove(licensePlate);
        if (ticket == null) return Optional.empty();
        double fee = feeCalculator.calculateFee(ticket, LocalDateTime.now());
        ticket.getLevel().releaseSpot(ticket.getSpot().getSpotId());
        return Optional.of(fee);
    }
}
```

### Verification

```
Setup: 1 level with spots: L1-S1 (SMALL), L1-M1 (MEDIUM), L1-L1 (LARGE)
FeeCalculator: HourlyFeeCalculator

Step 1: park(Car "ABC-123")
  activeTickets empty → not duplicate
  Level 1: L1-S1 → canFit(Car)? No (SMALL). L1-M1 → canFit(Car)? Yes (MEDIUM).
  tryPark → lock acquired, not occupied → occupied=true → return L1-M1
  Ticket T-1 created, stored in activeTickets["ABC-123"]

Step 2: park(Car "ABC-123") again
  activeTickets contains "ABC-123" → return empty (duplicate)

Step 3: park(Motorcycle "MOTO-1")
  L1-S1 → canFit(Motorcycle)? Yes. tryPark → success → Ticket T-2

Step 4: unpark("ABC-123") after 2.5 hours
  Ticket found, removed. ceilHours(2.5) = 3. Rate CAR = $2. Fee = $6.
  L1-M1 released → occupied=false

Step 5: park(Truck "TRK-1")
  L1-S1 occupied. L1-M1 → canFit(Truck)? No (MEDIUM). L1-L1 → canFit(Truck)? Yes (LARGE).
  tryPark → success → Ticket T-3
```

---

## Extensibility

### 1. "How would you support dynamic pricing by time of day?"

> "I'd create a new `DynamicFeeCalculator` implementing the same `FeeCalculator` interface. It would take peak and off-peak rates, and determine the rate based on the hour of exit. ParkingLot doesn't change — it just gets constructed with a different strategy."

```
class DynamicFeeCalculator implements FeeCalculator:
    - peakRate: Map<VehicleType, Double>
    - offPeakRate: Map<VehicleType, Double>

    + calculateFee(ticket, exitTime):
        rate = isPeakHour(exitTime) ? peakRate : offPeakRate
        return ceilHours * rate[vehicle.type]
```

### 2. "What about monthly passes?"

> "I'd add a `MonthlyPass` entity linking a license plate to a valid date range. In `unpark()`, before calculating the fee, check if the vehicle has an active pass. If so, the fee is zero. This is a short-circuit check that doesn't affect the rest of the flow."

```
class MonthlyPass:
    - licensePlate: String
    - validFrom: LocalDate
    - validTo: LocalDate

// In ParkingLot.unpark():
if hasActivePass(licensePlate)
    fee = 0.0
else
    fee = feeCalculator.calculateFee(ticket, now)
```

### 3. "What about EV charging spots?"

> "I'd extend Spot with an `EVSpot` subclass that adds a charging rate and a method to calculate charging cost based on duration. The fee calculator would check if the spot is an EVSpot and add the charging fee to the parking fee. Vehicle-to-spot matching still works through `canFit()` — an EV spot is just a regular spot with extra capability."

```
class EVSpot extends Spot:
    - chargingRatePerHour: double

    + getChargingFee(hours) → double
```

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can decompose the parking lot into logical pieces. You should identify spots with sizes, vehicles with types, and some mapping between them. A single-level implementation with a basic HashMap for tracking is fine. Fee calculation might be simple (flat rate). Thread safety isn't expected. If your system correctly parks, unparks, and rejects full-lot or wrong-size scenarios, you're doing well.

### Mid-level

For mid-level candidates, I expect multi-level support, proper vehicle-to-spot matching rules (motorcycle fits any, car fits medium/large, truck fits large only), and ceiling-hour fee calculation. ReentrantLock per spot for thread safety is a strong signal. The Strategy pattern for FeeCalculator shows good separation of concerns. You should be able to discuss extensibility and explain why Spot owns its own lock rather than using a global lock.

### Senior

Senior candidates should produce a thread-safe design with clear justification for locking granularity — why ReentrantLock per spot with `tryLock()` rather than synchronized blocks, why `volatile` on the occupied flag for fast reads. You'd explain the two-filter approach in `findAndPark` (cheap availability check first, then lock acquisition). Extensibility discussions should cover dynamic pricing with tradeoffs, monthly passes, and EV spots. You might also discuss capacity tracking per level for O(1) "is level full" checks.