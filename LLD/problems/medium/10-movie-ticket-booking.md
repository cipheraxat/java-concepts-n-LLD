# Movie Ticket Booking System

**Difficulty:** Medium | **Companies:** BookMyShow, Fandango, Amazon

---

## Requirements

> "Design the backend for a movie ticket booking system. Users browse shows, select seats, and book tickets. The system must handle concurrent seat selection and temporary holds."

### Clarifying Questions

> **You:** "Single theater or multi-cinema?"
>
> **Interviewer:** "A cinema with multiple screens. Each screen has a fixed seating layout."

> **You:** "How do we handle two users trying to book the same seat?"
>
> **Interviewer:** "Use a temporary hold — when a user selects a seat it's held for 10 minutes. If not confirmed, it releases."

> **You:** "Do we need different pricing?"
>
> **Interviewer:** "Yes — price can vary by seat type and show timing. Use a strategy."

> **You:** "Payment processing?"
>
> **Interviewer:** "Out of scope. Assume payment succeeds. Focus on seat locking and booking flow."

### Final Requirements

```
Requirements:
1. Cinema has multiple screens, each with a fixed seat layout
2. Shows are scheduled on a screen at a specific time for a movie
3. Users browse shows, view seat availability, and select seats
4. Selected seats are temporarily held (10-min expiry)
5. User confirms booking → seats become BOOKED
6. If hold expires → seats released back to AVAILABLE
7. Pluggable pricing strategy (by seat type, show timing)

Out of scope:
- Payment processing
- User authentication
- Seat layout design/rendering
- Discounts / promotions
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **Cinema** | Top-level facade. Owns screens and a show schedule. |
| **Screen** | Physical auditorium with a seating grid. |
| **Seat** | A seat in a screen with a type (REGULAR, PREMIUM, VIP). Immutable — defined by screen layout. |
| **Movie** | Movie metadata (title, duration). |
| **Show** | A screening of a movie on a screen at a time. Owns a map of ShowSeat (per-show seat state). |
| **ShowSeat** | Mutable seat state for a particular show: AVAILABLE → HELD → BOOKED. Owns the lock for concurrent access. |
| **Booking** | Confirmed ticket: links a user to a show and a list of seats with total price. |
| **PriceCalculator** | Strategy interface for computing ticket price per seat. |

---

## Class Design

### Show & ShowSeat

| Requirement | What Show must track |
|-------------|---------------------|
| "View seat availability" | Map of seatId → ShowSeat with statuses |
| "Temporary hold with expiry" | ShowSeat stores holdExpiry timestamp |
| "Concurrent seat selection" | Lock per ShowSeat |

```
class Show:
    - showId: String
    - movie: Movie
    - screen: Screen
    - startTime: LocalDateTime
    - seatMap: Map<String, ShowSeat>

    + getAvailableSeats() → List<ShowSeat>
    + holdSeats(seatIds, userId) → boolean
    + confirmBooking(seatIds, userId) → boolean

class ShowSeat:
    - seat: Seat
    - status: SeatStatus
    - heldBy: String?
    - holdExpiry: Instant?
    - lock: ReentrantLock

    + tryHold(userId, duration) → boolean
    + confirm(userId) → boolean
    + releaseIfExpired() → void

enum SeatStatus: AVAILABLE, HELD, BOOKED
```

### Cinema

```
class Cinema:
    - name: String
    - screens: List<Screen>
    - shows: Map<String, Show>
    - bookings: Map<String, Booking>
    - priceCalculator: PriceCalculator
    - bookingCounter: AtomicLong

    + getShowsForMovie(movieTitle) → List<Show>
    + holdSeats(showId, seatIds, userId) → boolean
    + confirmBooking(showId, seatIds, userId) → Optional<Booking>
```

### PriceCalculator

```
interface PriceCalculator:
    + calculatePrice(show, seat) → double

class StandardPriceCalculator implements PriceCalculator:
    - basePrices: Map<SeatType, Double>
    - peakMultiplier: double
    + calculatePrice(show, seat) →
        base = basePrices[seat.type]
        if isPeakHour(show.startTime) → base × peakMultiplier
        else → base
```

### Final Class Design

```
class Cinema: facade — holds screens, shows, bookings
class Screen: screenId, name, seats (layout)
class Seat: seatId, row, number, type (REGULAR/PREMIUM/VIP)
class Movie: title, durationMinutes
class Show: showId, movie, screen, startTime, Map<seatId, ShowSeat>
class ShowSeat: seat, status, heldBy, holdExpiry, ReentrantLock
class Booking: bookingId, userId, show, seats, totalPrice, timestamp
interface PriceCalculator → calculatePrice(show, seat)
```

---

## Implementation

### ShowSeat.tryHold

**Core logic:** Lock the seat, check if available (or if a previous hold expired), then mark as HELD with an expiry.

```
tryHold(userId, holdDuration)
    lock.lock()
    try:
        releaseIfExpired()
        if status != AVAILABLE → return false
        status = HELD
        heldBy = userId
        holdExpiry = now + holdDuration
        return true
    finally:
        lock.unlock()
```

### ShowSeat.confirm

```
confirm(userId)
    lock.lock()
    try:
        if status != HELD or heldBy != userId → return false
        if holdExpiry has passed → releaseIfExpired(); return false
        status = BOOKED
        heldBy = null
        holdExpiry = null
        return true
    finally:
        lock.unlock()
```

### Cinema.confirmBooking

```
confirmBooking(showId, seatIds, userId)
    show = shows[showId]
    confirmedSeats = []
    for each seatId in seatIds:
        showSeat = show.seatMap[seatId]
        if !showSeat.confirm(userId) → rollback all confirmed, return empty
        confirmedSeats.add(showSeat)

    totalPrice = confirmedSeats.sum(s → priceCalculator.calculatePrice(show, s.seat))
    booking = new Booking(nextId(), userId, show, confirmedSeats, totalPrice)
    bookings[booking.id] = booking
    return booking
```

### Complete Code Implementation

```java
public enum SeatType { REGULAR, PREMIUM, VIP }
public enum SeatStatus { AVAILABLE, HELD, BOOKED }
```

```java
public class Seat {
    private final String seatId;
    private final int row;
    private final int number;
    private final SeatType type;

    public Seat(String seatId, int row, int number, SeatType type) {
        this.seatId = seatId;
        this.row = row;
        this.number = number;
        this.type = type;
    }

    public String getSeatId() { return seatId; }
    public SeatType getType() { return type; }
}
```

```java
public class Movie {
    private final String title;
    private final int durationMinutes;

    public Movie(String title, int durationMinutes) {
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public String getTitle() { return title; }
    public int getDurationMinutes() { return durationMinutes; }
}
```

```java
import java.util.List;

public class Screen {
    private final String screenId;
    private final String name;
    private final List<Seat> seats;

    public Screen(String screenId, String name, List<Seat> seats) {
        this.screenId = screenId;
        this.name = name;
        this.seats = seats;
    }

    public String getScreenId() { return screenId; }
    public List<Seat> getSeats() { return seats; }
}
```

```java
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public class ShowSeat {
    private final Seat seat;
    private SeatStatus status;
    private String heldBy;
    private Instant holdExpiry;
    private final ReentrantLock lock = new ReentrantLock();

    public ShowSeat(Seat seat) {
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
    }

    public boolean tryHold(String userId, Duration holdDuration) {
        lock.lock();
        try {
            releaseIfExpired();
            if (status != SeatStatus.AVAILABLE) return false;
            status = SeatStatus.HELD;
            heldBy = userId;
            holdExpiry = Instant.now().plus(holdDuration);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean confirm(String userId) {
        lock.lock();
        try {
            if (status != SeatStatus.HELD || !userId.equals(heldBy)) return false;
            if (Instant.now().isAfter(holdExpiry)) {
                releaseIfExpired();
                return false;
            }
            status = SeatStatus.BOOKED;
            heldBy = null;
            holdExpiry = null;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void releaseIfExpired() {
        lock.lock();
        try {
            if (status == SeatStatus.HELD && holdExpiry != null
                    && Instant.now().isAfter(holdExpiry)) {
                status = SeatStatus.AVAILABLE;
                heldBy = null;
                holdExpiry = null;
            }
        } finally {
            lock.unlock();
        }
    }

    public Seat getSeat()         { return seat; }
    public SeatStatus getStatus() { return status; }
    public boolean isAvailable()  {
        lock.lock();
        try {
            releaseIfExpired();
            return status == SeatStatus.AVAILABLE;
        } finally {
            lock.unlock();
        }
    }
}
```

```java
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Show {
    private final String showId;
    private final Movie movie;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final Map<String, ShowSeat> seatMap;

    public Show(String showId, Movie movie, Screen screen, LocalDateTime startTime) {
        this.showId = showId;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.seatMap = new HashMap<>();
        for (Seat seat : screen.getSeats()) {
            seatMap.put(seat.getSeatId(), new ShowSeat(seat));
        }
    }

    public List<ShowSeat> getAvailableSeats() {
        return seatMap.values().stream()
            .filter(ShowSeat::isAvailable)
            .collect(Collectors.toList());
    }

    public boolean holdSeats(List<String> seatIds, String userId, Duration holdDuration) {
        for (String seatId : seatIds) {
            ShowSeat ss = seatMap.get(seatId);
            if (ss == null || !ss.tryHold(userId, holdDuration)) {
                // Rollback previously held seats
                for (String held : seatIds) {
                    if (held.equals(seatId)) break;
                    seatMap.get(held).releaseIfExpired(); // force release
                }
                return false;
            }
        }
        return true;
    }

    public String getShowId()           { return showId; }
    public Movie getMovie()             { return movie; }
    public LocalDateTime getStartTime() { return startTime; }
    public Map<String, ShowSeat> getSeatMap() { return seatMap; }
}
```

```java
import java.time.LocalDateTime;

public interface PriceCalculator {
    double calculatePrice(Show show, Seat seat);
}

public class StandardPriceCalculator implements PriceCalculator {
    private final double regularPrice;
    private final double premiumPrice;
    private final double vipPrice;
    private final double peakMultiplier;

    public StandardPriceCalculator(double regularPrice, double premiumPrice,
                                    double vipPrice, double peakMultiplier) {
        this.regularPrice = regularPrice;
        this.premiumPrice = premiumPrice;
        this.vipPrice = vipPrice;
        this.peakMultiplier = peakMultiplier;
    }

    @Override
    public double calculatePrice(Show show, Seat seat) {
        double base = switch (seat.getType()) {
            case REGULAR -> regularPrice;
            case PREMIUM -> premiumPrice;
            case VIP     -> vipPrice;
        };
        if (isPeakHour(show.getStartTime())) {
            base *= peakMultiplier;
        }
        return base;
    }

    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= 18 && hour <= 22;
    }
}
```

```java
import java.time.Instant;
import java.util.List;

public class Booking {
    private final String bookingId;
    private final String userId;
    private final Show show;
    private final List<ShowSeat> seats;
    private final double totalPrice;
    private final Instant timestamp;

    public Booking(String bookingId, String userId, Show show,
                   List<ShowSeat> seats, double totalPrice) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.show = show;
        this.seats = seats;
        this.totalPrice = totalPrice;
        this.timestamp = Instant.now();
    }

    public String getBookingId()   { return bookingId; }
    public String getUserId()      { return userId; }
    public Show getShow()          { return show; }
    public List<ShowSeat> getSeats() { return seats; }
    public double getTotalPrice()  { return totalPrice; }
}
```

```java
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Cinema {
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private final String name;
    private final List<Screen> screens = new ArrayList<>();
    private final Map<String, Show> shows = new HashMap<>();
    private final Map<String, Booking> bookings = new HashMap<>();
    private final PriceCalculator priceCalculator;
    private final AtomicLong bookingCounter = new AtomicLong(1);

    public Cinema(String name, PriceCalculator priceCalculator) {
        this.name = name;
        this.priceCalculator = priceCalculator;
    }

    public void addScreen(Screen screen) { screens.add(screen); }
    public void addShow(Show show) { shows.put(show.getShowId(), show); }

    public List<Show> getShowsForMovie(String movieTitle) {
        return shows.values().stream()
            .filter(s -> s.getMovie().getTitle().equalsIgnoreCase(movieTitle))
            .collect(Collectors.toList());
    }

    public boolean holdSeats(String showId, List<String> seatIds, String userId) {
        Show show = shows.get(showId);
        if (show == null) return false;
        return show.holdSeats(seatIds, userId, HOLD_DURATION);
    }

    public Optional<Booking> confirmBooking(String showId, List<String> seatIds,
                                             String userId) {
        Show show = shows.get(showId);
        if (show == null) return Optional.empty();

        List<ShowSeat> confirmed = new ArrayList<>();
        for (String seatId : seatIds) {
            ShowSeat ss = show.getSeatMap().get(seatId);
            if (ss == null || !ss.confirm(userId)) {
                // Rollback: release already-confirmed seats back (mark available)
                // In practice, confirmed seats can't easily be undone —
                // so hold + confirm should be an atomic two-phase flow.
                return Optional.empty();
            }
            confirmed.add(ss);
        }

        double total = confirmed.stream()
            .mapToDouble(ss -> priceCalculator.calculatePrice(show, ss.getSeat()))
            .sum();

        Booking booking = new Booking(
            "B-" + bookingCounter.getAndIncrement(), userId, show, confirmed, total);
        bookings.put(booking.getBookingId(), booking);
        return Optional.of(booking);
    }
}
```

### Verification

```
Setup: Cinema with Screen S1 (seats: A1-REGULAR, A2-REGULAR, B1-VIP).
Show SH1: "Inception" on S1 at 7 PM. StandardPriceCalculator(100, 150, 250, 1.5x peak).

Step 1: Alice views available seats for SH1
  getAvailableSeats() → [A1, A2, B1] (all AVAILABLE)

Step 2: Alice holds [A1, B1]
  A1.tryHold("Alice", 10min): AVAILABLE → HELD ✓
  B1.tryHold("Alice", 10min): AVAILABLE → HELD ✓
  holdExpiry = now + 10min

Step 3: Bob tries to hold [A1]
  A1.tryHold("Bob"): status=HELD, not expired → return false

Step 4: Alice confirms [A1, B1]
  A1.confirm("Alice"): HELD, heldBy=Alice, not expired → BOOKED ✓
  B1.confirm("Alice"): HELD, heldBy=Alice, not expired → BOOKED ✓
  Price: A1 = 100 × 1.5 (7PM peak) = $150
         B1 = 250 × 1.5 = $375
  Total = $525. Booking B-1 created.

Step 5: (Simulated) 10 min pass, unconfirmed holds expire
  A2 was never held → still AVAILABLE
  If someone had held A2 without confirming, releaseIfExpired() would free it.
```

---

## Extensibility

### 1. "How would you add booking cancellation with refunds?"

> "I'd add a `cancelBooking(bookingId)` method on Cinema. It marks each ShowSeat as AVAILABLE again and applies a refund policy (Strategy pattern, similar to Hotel Booking). The refund amount depends on how far in advance the cancellation occurs relative to the show start time."

### 2. "How would you handle seat selection timeouts at scale?"

> "Instead of checking expiry lazily on each access, I'd run a background `ScheduledExecutorService` that scans HELD seats periodically (every 30 seconds) and releases expired holds. This prevents stale holds from blocking seats when no one accesses them."

### 3. "How would you support different screen layouts?"

> "I'd make screen layout a configurable structure — perhaps a `LayoutBuilder` that generates rows and seats from a JSON config. Each Seat already has row/number/type, so varying layouts only changes how Seats are constructed, not how the booking logic works."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model Show, Seat, and Booking clearly. A basic booking flow that marks seats as booked and rejects already-booked seats is the main goal. Hold/expiry logic is not required. Price calculation can be flat.

### Mid-level

Mid-level candidates should implement the two-phase hold-then-confirm flow with per-seat locking via ReentrantLock. The hold expiry mechanism should be in place. A Strategy pattern for price calculation is expected. You should handle the rollback case when a multi-seat hold partially fails.

### Senior

Senior candidates would discuss the trade-offs of per-seat vs. per-show locking granularity. You'd explain how to prevent orphaned holds at scale (background expiry sweeper vs. lazy expiry). The confirmation should be atomic — either all seats confirm or none do. You'd propose solutions for high-contention scenarios like popular show launches.