# Hotel Booking System

**Difficulty:** Medium | **Companies:** Airbnb, Booking.com, Expedia

---

## Requirements

> "Design the backend for a hotel booking system. Guests can search for rooms, make reservations for date ranges, cancel bookings, and the hotel can manage room inventory."

### Clarifying Questions

> **You:** "Single hotel or multi-hotel chain?"
>
> **Interviewer:** "Single hotel with multiple room types."

> **You:** "What room types?"
>
> **Interviewer:** "SINGLE, DOUBLE, DELUXE, SUITE — each with a base nightly rate."

> **You:** "How do we handle overlapping dates?"
>
> **Interviewer:** "A room can only have one reservation per night. No overlaps."

> **You:** "Any cancellation policy?"
>
> **Interviewer:** "Support different cancellation policies — free cancellation before a deadline, partial refund, or no refund."

> **You:** "Do we need pricing strategies?"
>
> **Interviewer:** "Yes — flat rate and seasonal pricing. Use a strategy pattern."

### Final Requirements

```
Requirements:
1. Rooms have types (SINGLE, DOUBLE, DELUXE, SUITE) and nightly rates
2. Search available rooms by type and date range
3. Book a room for a date range (no overlapping bookings per room)
4. Cancel a reservation (applies cancellation policy)
5. Pluggable pricing strategy (flat, seasonal)
6. Pluggable cancellation policy

Out of scope:
- Multi-hotel chains
- Payment processing
- Room amenities / photos
- Guest reviews
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **Hotel** | Facade. Owns rooms and reservations. Coordinates search, booking, and cancellation flows. |
| **Room** | Physical room with a type and list of reservations. Checks availability by date-range overlap detection. |
| **RoomType** | Enum: SINGLE, DOUBLE, DELUXE, SUITE. |
| **DateRange** | Value object encapsulating check-in and check-out dates. Provides overlap detection logic. |
| **Reservation** | Links a guest to a room for a date range. Has a status (CONFIRMED, CANCELLED). |
| **Guest** | Name and contact info. |
| **PricingStrategy** | Interface for calculating total price for a room and date range. |
| **CancellationPolicy** | Interface for calculating refund amount on cancellation. |

---

## Class Design

### Hotel

| Requirement | What Hotel must track |
|-------------|----------------------|
| "Search available rooms by type and date range" | List of all rooms |
| "Book / cancel" | Map of reservationId → Reservation |
| "Pluggable pricing and cancellation" | Strategy references |

```
class Hotel:
    - name: String
    - rooms: List<Room>
    - reservations: Map<String, Reservation>
    - pricingStrategy: PricingStrategy
    - cancellationPolicy: CancellationPolicy
    - reservationCounter: AtomicLong

    + searchAvailableRooms(type, dateRange) → List<Room>
    + bookRoom(guest, roomId, dateRange) → Optional<Reservation>
    + cancelReservation(reservationId) → double
    + setPricingStrategy(strategy) → void
    + setCancellationPolicy(policy) → void
```

### Room

| Requirement | What Room must track |
|-------------|---------------------|
| "No overlapping bookings" | List of active reservations to check overlaps |

```
class Room:
    - roomId: String
    - type: RoomType
    - baseRate: double
    - reservations: List<Reservation>

    + isAvailable(dateRange) → boolean
    + addReservation(reservation) → void
    + removeReservation(reservation) → void
```

### DateRange

```
class DateRange:
    - checkIn: LocalDate
    - checkOut: LocalDate

    + overlaps(other: DateRange) → boolean
    + getNights() → long
```

### Reservation

```
class Reservation:
    - reservationId: String
    - guest: Guest
    - room: Room
    - dateRange: DateRange
    - totalPrice: double
    - status: ReservationStatus

    + cancel() → void

enum ReservationStatus: CONFIRMED, CANCELLED
```

### PricingStrategy & CancellationPolicy

```
interface PricingStrategy:
    + calculatePrice(room, dateRange) → double

class FlatPricingStrategy implements PricingStrategy:
    + calculatePrice(room, dateRange) → room.baseRate × dateRange.getNights()

class SeasonalPricingStrategy implements PricingStrategy:
    - seasonalRates: Map<Month, Double>  // multipliers
    + calculatePrice(room, dateRange) → sum of (baseRate × multiplier) per night

interface CancellationPolicy:
    + calculateRefund(reservation, cancellationDate) → double

class FreeCancellationPolicy implements CancellationPolicy:
    - deadline: int  // days before check-in
    + calculateRefund(reservation, cancellationDate) → full if before deadline, else 0

class PartialRefundPolicy implements CancellationPolicy:
    - refundPercentage: double
    + calculateRefund(reservation, cancellationDate) → totalPrice × refundPercentage
```

### Final Class Design

```
class Hotel:
    - rooms, reservations, pricingStrategy, cancellationPolicy
    + searchAvailableRooms(type, dateRange)
    + bookRoom(guest, roomId, dateRange)
    + cancelReservation(reservationId)

class Room:
    - roomId, type, baseRate, reservations
    + isAvailable(dateRange) → boolean

class DateRange:
    - checkIn, checkOut
    + overlaps(other) → boolean
    + getNights() → long

class Reservation:
    - reservationId, guest, room, dateRange, totalPrice, status

class Guest:
    - guestId, name, email

interface PricingStrategy → calculatePrice(room, dateRange)
interface CancellationPolicy → calculateRefund(reservation, cancellationDate)
```

---

## Implementation

### DateRange.overlaps

**Core logic:** Two date ranges overlap if one starts before the other ends and vice versa.

```
overlaps(other)
    return checkIn < other.checkOut AND other.checkIn < checkOut
```

### Hotel.searchAvailableRooms

```
searchAvailableRooms(type, dateRange)
    return rooms.stream()
        .filter(r → r.type == type)
        .filter(r → r.isAvailable(dateRange))
        .collect(toList)
```

### Hotel.bookRoom

**Core logic:**
1. Find the room by ID
2. Check availability for the date range
3. Calculate price using strategy
4. Create reservation and add to room

```
bookRoom(guest, roomId, dateRange)
    room = findRoom(roomId)
    if room is null or !room.isAvailable(dateRange) → return empty

    price = pricingStrategy.calculatePrice(room, dateRange)
    reservation = new Reservation(nextId(), guest, room, dateRange, price)
    room.addReservation(reservation)
    reservations[reservation.id] = reservation
    return reservation
```

### Hotel.cancelReservation

```
cancelReservation(reservationId)
    reservation = reservations[reservationId]
    if reservation is null or reservation.status == CANCELLED → return 0.0

    refund = cancellationPolicy.calculateRefund(reservation, today)
    reservation.cancel()
    reservation.room.removeReservation(reservation)
    return refund
```

### Complete Code Implementation

```java
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateRange {
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public DateRange(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn))
            throw new IllegalArgumentException("Check-out must be after check-in");
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }

    public long getNights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
    public LocalDate getCheckIn()  { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
}
```

```java
public enum RoomType { SINGLE, DOUBLE, DELUXE, SUITE }

public enum ReservationStatus { CONFIRMED, CANCELLED }
```

```java
public class Guest {
    private final String guestId;
    private final String name;
    private final String email;

    public Guest(String guestId, String name, String email) {
        this.guestId = guestId;
        this.name = name;
        this.email = email;
    }

    public String getGuestId() { return guestId; }
    public String getName()    { return name; }
    public String getEmail()   { return email; }
}
```

```java
public class Reservation {
    private final String reservationId;
    private final Guest guest;
    private final Room room;
    private final DateRange dateRange;
    private final double totalPrice;
    private ReservationStatus status;

    public Reservation(String reservationId, Guest guest, Room room,
                       DateRange dateRange, double totalPrice) {
        this.reservationId = reservationId;
        this.guest = guest;
        this.room = room;
        this.dateRange = dateRange;
        this.totalPrice = totalPrice;
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() { this.status = ReservationStatus.CANCELLED; }

    public String getReservationId() { return reservationId; }
    public Guest getGuest()          { return guest; }
    public Room getRoom()            { return room; }
    public DateRange getDateRange()  { return dateRange; }
    public double getTotalPrice()    { return totalPrice; }
    public ReservationStatus getStatus() { return status; }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Room {
    private final String roomId;
    private final RoomType type;
    private final double baseRate;
    private final List<Reservation> reservations;

    public Room(String roomId, RoomType type, double baseRate) {
        this.roomId = roomId;
        this.type = type;
        this.baseRate = baseRate;
        this.reservations = new ArrayList<>();
    }

    public boolean isAvailable(DateRange dateRange) {
        return reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
            .noneMatch(r -> r.getDateRange().overlaps(dateRange));
    }

    public void addReservation(Reservation r) { reservations.add(r); }
    public void removeReservation(Reservation r) { reservations.remove(r); }

    public String getRoomId()    { return roomId; }
    public RoomType getType()    { return type; }
    public double getBaseRate()  { return baseRate; }
}
```

```java
public interface PricingStrategy {
    double calculatePrice(Room room, DateRange dateRange);
}

public class FlatPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(Room room, DateRange dateRange) {
        return room.getBaseRate() * dateRange.getNights();
    }
}
```

```java
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

public class SeasonalPricingStrategy implements PricingStrategy {
    private final Map<Month, Double> multipliers = new HashMap<>();

    public void setMultiplier(Month month, double multiplier) {
        multipliers.put(month, multiplier);
    }

    @Override
    public double calculatePrice(Room room, DateRange dateRange) {
        double total = 0.0;
        LocalDate date = dateRange.getCheckIn();
        while (date.isBefore(dateRange.getCheckOut())) {
            double multiplier = multipliers.getOrDefault(date.getMonth(), 1.0);
            total += room.getBaseRate() * multiplier;
            date = date.plusDays(1);
        }
        return total;
    }
}
```

```java
import java.time.LocalDate;

public interface CancellationPolicy {
    double calculateRefund(Reservation reservation, LocalDate cancellationDate);
}

public class FreeCancellationPolicy implements CancellationPolicy {
    private final int daysBeforeCheckIn;

    public FreeCancellationPolicy(int daysBeforeCheckIn) {
        this.daysBeforeCheckIn = daysBeforeCheckIn;
    }

    @Override
    public double calculateRefund(Reservation reservation, LocalDate cancellationDate) {
        LocalDate deadline = reservation.getDateRange().getCheckIn()
            .minusDays(daysBeforeCheckIn);
        return cancellationDate.isBefore(deadline) || cancellationDate.isEqual(deadline)
            ? reservation.getTotalPrice() : 0.0;
    }
}

public class PartialRefundPolicy implements CancellationPolicy {
    private final double refundPercentage;

    public PartialRefundPolicy(double refundPercentage) {
        this.refundPercentage = refundPercentage;
    }

    @Override
    public double calculateRefund(Reservation reservation, LocalDate cancellationDate) {
        return reservation.getTotalPrice() * refundPercentage;
    }
}
```

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Hotel {
    private final String name;
    private final List<Room> rooms = new ArrayList<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private PricingStrategy pricingStrategy;
    private CancellationPolicy cancellationPolicy;
    private final AtomicLong counter = new AtomicLong(1);

    public Hotel(String name, PricingStrategy pricingStrategy,
                 CancellationPolicy cancellationPolicy) {
        this.name = name;
        this.pricingStrategy = pricingStrategy;
        this.cancellationPolicy = cancellationPolicy;
    }

    public List<Room> searchAvailableRooms(RoomType type, DateRange dateRange) {
        return rooms.stream()
            .filter(r -> r.getType() == type)
            .filter(r -> r.isAvailable(dateRange))
            .collect(Collectors.toList());
    }

    public Optional<Reservation> bookRoom(Guest guest, String roomId, DateRange dateRange) {
        Room room = rooms.stream()
            .filter(r -> r.getRoomId().equals(roomId))
            .findFirst().orElse(null);
        if (room == null || !room.isAvailable(dateRange)) return Optional.empty();

        double price = pricingStrategy.calculatePrice(room, dateRange);
        Reservation reservation = new Reservation(
            "R-" + counter.getAndIncrement(), guest, room, dateRange, price);
        room.addReservation(reservation);
        reservations.put(reservation.getReservationId(), reservation);
        return Optional.of(reservation);
    }

    public double cancelReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null || reservation.getStatus() == ReservationStatus.CANCELLED)
            return 0.0;

        double refund = cancellationPolicy.calculateRefund(
            reservation, LocalDate.now());
        reservation.cancel();
        reservation.getRoom().removeReservation(reservation);
        return refund;
    }

    public void addRoom(Room room) { rooms.add(room); }
    public void setPricingStrategy(PricingStrategy s)   { this.pricingStrategy = s; }
    public void setCancellationPolicy(CancellationPolicy p) { this.cancellationPolicy = p; }
}
```

### Verification

```
Setup: Hotel with 2 DELUXE rooms (D1 $200/night, D2 $200/night).
Flat pricing. Free-cancellation (3 days before check-in).
Guest: Alice.

Step 1: Search DELUXE rooms for Jan 10–13 (3 nights)
  rooms.stream() → D1 (no reservations → available), D2 (available)
  Result: [D1, D2]

Step 2: Alice books D1 for Jan 10–13
  room=D1 found, isAvailable(Jan10-13) = true
  price = $200 × 3 = $600
  Reservation R-1 created, D1.reservations=[R-1]

Step 3: Search DELUXE for Jan 11–14 (overlaps R-1)
  D1: R-1 dateRange Jan10-13, overlap check: Jan11 < Jan13 AND Jan10 < Jan14 → true → not available
  D2: no reservations → available
  Result: [D2]

Step 4: Search DELUXE for Jan 13–15 (adjacent, no overlap)
  D1: overlap check: Jan13 < Jan13 → false → abutting is OK → available
  Result: [D1, D2]

Step 5: Cancel R-1 on Jan 5 (5 days before, deadline = Jan 7)
  Jan 5 ≤ Jan 7 → full refund = $600
  R-1.cancel(), D1.reservations=[]
```

---

## Extensibility

### 1. "How would you add seasonal pricing?"

> "Already supported via the Strategy pattern. I'd inject a `SeasonalPricingStrategy` that holds a `Map<Month, Double>` of multipliers. The `calculatePrice` method walks each night in the date range, looks up the month's multiplier, and sums `baseRate × multiplier`. Switching strategies is a single setter call — no booking logic changes."

### 2. "How would you handle room upgrades?"

> "I'd add a method `upgradeRoom(reservationId, newRoomId)` on Hotel. It checks the new room is available for the same date range, recalculates the price difference, cancels the old reservation, and creates a new one. The guest pays only the delta."

### 3. "How would you support group bookings?"

> "I'd create a `GroupReservation` that wraps multiple `Reservation` objects booked atomically. The `bookGroup(guest, roomIds, dateRange)` method verifies all rooms are available before committing any. If any room is unavailable, none are booked — essentially an all-or-nothing transaction."

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm looking for working Room and Reservation classes with a clear date-range model. You should be able to determine availability by checking for overlapping dates. A simple booking flow that rejects conflicts is the main goal. Cancellation can be basic (status flip).

### Mid-level

Mid-level candidates should use the Strategy pattern for pricing and cancellation policies. The DateRange overlap logic should be concise and correct (start-before-end check). You'd separate the Hotel facade from Room-level logic, and handle edge cases like abutting date ranges not conflicting.

### Senior

Senior candidates would discuss atomicity for concurrent bookings (two guests trying to book the same room simultaneously), potentially using locking on the Room object. You'd explain the trade-off between optimistic vs. pessimistic concurrency. Extensibility answers should be concrete — group bookings as atomic transactions, room upgrades as cancel-and-rebook with delta pricing.