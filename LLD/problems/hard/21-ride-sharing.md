# Ride Sharing Service

**Difficulty:** Hard | **Companies:** Uber, Lyft, Grab, Ola

---

## Requirements

> "Design a ride-sharing service like Uber that matches riders with nearby drivers, tracks trips, and calculates fares."

### Clarifying Questions

> **You:** "How do we find the nearest available driver?"
>
> **Interviewer:** "Use Haversine distance. Find drivers within a configurable radius sorted by proximity."

> **You:** "What ride types do we support?"
>
> **Interviewer:** "Standard and Premium for now, with different fare multipliers."

> **You:** "How do we handle the trip lifecycle?"
>
> **Interviewer:** "REQUESTED → DRIVER_ASSIGNED → IN_PROGRESS → COMPLETED or CANCELLED. A state machine."

> **You:** "Concurrency — multiple riders requesting the same driver?"
>
> **Interviewer:** "Use a ReentrantLock on Driver to atomically assign. Only one rider wins."

### Final Requirements

```
Requirements:
1. Rider requests a trip with pickup and dropoff locations
2. Find nearest available driver within radius (Haversine)
3. Driver assignment with ReentrantLock (tryAssign)
4. Trip state machine: REQUESTED → ASSIGNED → IN_PROGRESS → COMPLETED/CANCELLED
5. Fare calculation via strategy (base + per-km + per-min)
6. RideType: STANDARD, PREMIUM with fare multipliers
7. Driver and Rider profiles

Out of scope:
- Surge pricing
- Real-time GPS tracking
- Payment processing
- Ride pooling / shared rides
- Route optimization
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **RideService** | Facade. Coordinates trip requests, driver matching, fare calculation. |
| **Rider** | Holds rider info and current trip reference. |
| **Driver** | Holds location, availability status, ReentrantLock for atomic assignment. |
| **Trip** | Tracks state machine, rider, driver, locations, timestamps, fare. |
| **Location** | Latitude/longitude pair. Provides Haversine distance calculation. |
| **FareCalculator** | Strategy interface for fare computation. |
| **StandardFareCalculator** | Base + per-km + per-min with ride-type multiplier. |
| **RideType** | Enum: STANDARD, PREMIUM. |
| **TripStatus** | Enum: REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED. |

---

## Class Design

### Location

```
class Location:
    - latitude: double
    - longitude: double

    + distanceTo(other: Location) → double  // Haversine in km
```

### Driver

```
class Driver:
    - driverId: String
    - name: String
    - location: Location
    - available: boolean
    - currentTrip: Trip?
    - lock: ReentrantLock

    + tryAssign(trip) → boolean  // lock, check available, assign
    + release() → void
    + updateLocation(location) → void
```

### Trip

```
class Trip:
    - tripId: String
    - rider: Rider
    - driver: Driver?
    - pickup: Location
    - dropoff: Location
    - rideType: RideType
    - status: TripStatus
    - requestTime: Instant
    - startTime: Instant?
    - endTime: Instant?
    - fare: double

    + assignDriver(driver) → void
    + start() → void
    + complete(fare) → void
    + cancel() → void
```

### FareCalculator (Strategy)

```
interface FareCalculator:
    + calculate(trip) → double

class StandardFareCalculator implements FareCalculator:
    - baseFare: double
    - perKmRate: double
    - perMinRate: double
    - premiumMultiplier: double

    + calculate(trip) → double
```

### RideService

```
class RideService:
    - drivers: List<Driver>
    - trips: Map<String, Trip>
    - fareCalculator: FareCalculator
    - maxRadiusKm: double

    + requestTrip(rider, pickup, dropoff, rideType) → Trip
    + startTrip(tripId) → void
    + completeTrip(tripId) → void
    + cancelTrip(tripId) → void
    - findNearestDriver(pickup, rideType) → Driver?
```

---

## Implementation

### Location.distanceTo (Haversine formula)

```
distanceTo(other)
    R = 6371 km
    dLat = toRadians(other.lat - this.lat)
    dLon = toRadians(other.lon - this.lon)
    a = sin(dLat/2)^2 + cos(lat1)*cos(lat2)*sin(dLon/2)^2
    c = 2 * atan2(sqrt(a), sqrt(1-a))
    return R * c
```

### Driver.tryAssign (atomic assignment)

```
tryAssign(trip)
    lock.lock()
    try:
        if not available → return false
        available = false
        currentTrip = trip
        return true
    finally:
        lock.unlock()
```

### RideService.requestTrip

```
requestTrip(rider, pickup, dropoff, rideType)
    trip = new Trip(rider, pickup, dropoff, rideType)
    driver = findNearestDriver(pickup, rideType)
    if driver is null → throw "No driver available"
    if driver.tryAssign(trip):
        trip.assignDriver(driver)
    else:
        // retry next closest — in real system, loop through candidates
        throw "Driver assignment failed"
    trips.put(trip.id, trip)
    return trip

findNearestDriver(pickup, rideType)
    candidates = drivers.stream()
        .filter(d → d.isAvailable())
        .filter(d → d.location.distanceTo(pickup) <= maxRadiusKm)
        .sorted(by distance to pickup)
    return candidates.findFirst()
```

### Complete Code Implementation

```java
public class Location {
    private final double latitude;
    private final double longitude;
    private static final double EARTH_RADIUS_KM = 6371.0;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double distanceTo(Location other) {
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(this.latitude))
            * Math.cos(Math.toRadians(other.latitude))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
```

```java
public enum RideType {
    STANDARD(1.0),
    PREMIUM(1.5);

    private final double multiplier;
    RideType(double multiplier) { this.multiplier = multiplier; }
    public double getMultiplier() { return multiplier; }
}
```

```java
public enum TripStatus {
    REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
}
```

```java
public class Rider {
    private final String riderId;
    private final String name;

    public Rider(String riderId, String name) {
        this.riderId = riderId;
        this.name = name;
    }

    public String getRiderId() { return riderId; }
    public String getName() { return name; }
}
```

```java
import java.util.concurrent.locks.ReentrantLock;

public class Driver {
    private final String driverId;
    private final String name;
    private Location location;
    private boolean available;
    private Trip currentTrip;
    private final ReentrantLock lock = new ReentrantLock();

    public Driver(String driverId, String name, Location location) {
        this.driverId = driverId;
        this.name = name;
        this.location = location;
        this.available = true;
    }

    public boolean tryAssign(Trip trip) {
        lock.lock();
        try {
            if (!available) return false;
            available = false;
            currentTrip = trip;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void release() {
        lock.lock();
        try {
            available = true;
            currentTrip = null;
        } finally {
            lock.unlock();
        }
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public boolean isAvailable() { return available; }
    public Trip getCurrentTrip() { return currentTrip; }
}
```

```java
import java.time.Instant;
import java.util.UUID;

public class Trip {
    private final String tripId;
    private final Rider rider;
    private Driver driver;
    private final Location pickup;
    private final Location dropoff;
    private final RideType rideType;
    private TripStatus status;
    private final Instant requestTime;
    private Instant startTime;
    private Instant endTime;
    private double fare;

    public Trip(Rider rider, Location pickup, Location dropoff, RideType rideType) {
        this.tripId = UUID.randomUUID().toString();
        this.rider = rider;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.rideType = rideType;
        this.status = TripStatus.REQUESTED;
        this.requestTime = Instant.now();
    }

    public void assignDriver(Driver driver) {
        if (status != TripStatus.REQUESTED)
            throw new IllegalStateException("Cannot assign driver in state: " + status);
        this.driver = driver;
        this.status = TripStatus.DRIVER_ASSIGNED;
    }

    public void start() {
        if (status != TripStatus.DRIVER_ASSIGNED)
            throw new IllegalStateException("Cannot start in state: " + status);
        this.startTime = Instant.now();
        this.status = TripStatus.IN_PROGRESS;
    }

    public void complete(double fare) {
        if (status != TripStatus.IN_PROGRESS)
            throw new IllegalStateException("Cannot complete in state: " + status);
        this.endTime = Instant.now();
        this.fare = fare;
        this.status = TripStatus.COMPLETED;
    }

    public void cancel() {
        if (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED)
            throw new IllegalStateException("Cannot cancel in state: " + status);
        this.status = TripStatus.CANCELLED;
        this.endTime = Instant.now();
    }

    public String getTripId() { return tripId; }
    public Rider getRider() { return rider; }
    public Driver getDriver() { return driver; }
    public Location getPickup() { return pickup; }
    public Location getDropoff() { return dropoff; }
    public RideType getRideType() { return rideType; }
    public TripStatus getStatus() { return status; }
    public Instant getRequestTime() { return requestTime; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public double getFare() { return fare; }

    public double getDistanceKm() {
        return pickup.distanceTo(dropoff);
    }

    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}
```

```java
public interface FareCalculator {
    double calculate(Trip trip);
}
```

```java
public class StandardFareCalculator implements FareCalculator {
    private final double baseFare;
    private final double perKmRate;
    private final double perMinRate;

    public StandardFareCalculator(double baseFare, double perKmRate, double perMinRate) {
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinRate = perMinRate;
    }

    @Override
    public double calculate(Trip trip) {
        double distance = trip.getDistanceKm();
        long minutes = trip.getDurationMinutes();
        double raw = baseFare + (perKmRate * distance) + (perMinRate * minutes);
        return Math.round(raw * trip.getRideType().getMultiplier() * 100.0) / 100.0;
    }
}
```

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RideService {
    private final List<Driver> drivers = new ArrayList<>();
    private final Map<String, Trip> trips = new ConcurrentHashMap<>();
    private final FareCalculator fareCalculator;
    private final double maxRadiusKm;

    public RideService(FareCalculator fareCalculator, double maxRadiusKm) {
        this.fareCalculator = fareCalculator;
        this.maxRadiusKm = maxRadiusKm;
    }

    public void addDriver(Driver driver) {
        drivers.add(driver);
    }

    public Trip requestTrip(Rider rider, Location pickup, Location dropoff,
                            RideType rideType) {
        Trip trip = new Trip(rider, pickup, dropoff, rideType);

        List<Driver> candidates = drivers.stream()
            .filter(Driver::isAvailable)
            .filter(d -> d.getLocation().distanceTo(pickup) <= maxRadiusKm)
            .sorted(Comparator.comparingDouble(
                d -> d.getLocation().distanceTo(pickup)))
            .collect(Collectors.toList());

        for (Driver candidate : candidates) {
            if (candidate.tryAssign(trip)) {
                trip.assignDriver(candidate);
                trips.put(trip.getTripId(), trip);
                return trip;
            }
        }
        throw new IllegalStateException("No driver available nearby");
    }

    public void startTrip(String tripId) {
        Trip trip = getTrip(tripId);
        trip.start();
    }

    public void completeTrip(String tripId) {
        Trip trip = getTrip(tripId);
        double fare = fareCalculator.calculate(trip);
        trip.complete(fare);
        trip.getDriver().release();
    }

    public void cancelTrip(String tripId) {
        Trip trip = getTrip(tripId);
        trip.cancel();
        if (trip.getDriver() != null) {
            trip.getDriver().release();
        }
    }

    private Trip getTrip(String tripId) {
        Trip trip = trips.get(tripId);
        if (trip == null)
            throw new IllegalArgumentException("Trip not found: " + tripId);
        return trip;
    }
}
```

### Verification

```
Setup:
  Driver D1 at (12.97, 77.59) — 2 km from pickup
  Driver D2 at (12.98, 77.60) — 1.5 km from pickup
  Driver D3 at (13.10, 77.70) — 15 km away (out of 10km radius)
  Rider R1. Pickup (12.975, 77.595). Dropoff (13.02, 77.63).
  FareCalculator: base=50, perKm=10, perMin=2. maxRadius=10km.

Step 1: requestTrip(R1, pickup, dropoff, STANDARD)
  Candidates within 10km: D2 (1.5km), D1 (2km). D3 excluded.
  Sorted: [D2, D1]. Try D2.tryAssign → true.
  Trip status = DRIVER_ASSIGNED, driver = D2 ✓

Step 2: startTrip(tripId)
  status DRIVER_ASSIGNED → IN_PROGRESS, startTime set ✓

Step 3: completeTrip(tripId)
  distance = ~5.5 km. duration = say 15 min.
  fare = (50 + 10*5.5 + 2*15) * 1.0 = 50 + 55 + 30 = 135.0
  Trip COMPLETED. D2 released (available=true) ✓

Step 4: requestTrip(R1, pickup, dropoff, PREMIUM)
  D2 now available again. D2.tryAssign → true.
  fare would be 135.0 * 1.5 = 202.50 ✓

Step 5: cancelTrip(tripId) — on trip in DRIVER_ASSIGNED state
  Trip CANCELLED. Driver released ✓
```

---

## Extensibility

### 1. "How would you add surge pricing?"

> "I'd create a SurgeCalculator that takes current demand (active trip requests) and driver supply per zone. The surge multiplier is applied in the FareCalculator. Zones are defined by geohash or grid cells. The surge factor is computed as demand/supply ratio capped at a maximum (e.g., 3x). This keeps the pricing strategy pluggable."

### 2. "How would you add ride pooling?"

> "A PooledTrip extends Trip and holds a list of rider-pickup-dropoff triples. The matching algorithm finds riders with overlapping routes (pickup/dropoff within a detour threshold). The driver picks up and drops off in an optimized order. Fare is split proportionally based on each rider's individual distance."

### 3. "How would you handle driver location updates in real-time?"

> "Drivers periodically send GPS updates. I'd use a spatial index (QuadTree or GeoHash grid) to store driver locations. On each update, the driver's entry moves between grid cells. For finding nearby drivers, the spatial index returns candidates in O(log n) instead of scanning all drivers."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should implement the core entities (Rider, Driver, Trip, Location) and the trip lifecycle state machine. Basic driver matching by iterating through all drivers and picking the nearest one is sufficient. Haversine distance should be implemented correctly. Concurrency is not required.

### Mid-level

Mid-level candidates should add the FareCalculator strategy pattern, ReentrantLock on Driver for atomic assignment with tryAssign, ride types with multipliers, and proper error handling for state transitions. The driver matching should loop through sorted candidates and handle assignment failures gracefully.

### Senior

Senior candidates would discuss spatial indexing (QuadTree, GeoHash) for efficient driver lookup, surge pricing architecture, handling driver disconnects and reassignment, and the trade-offs between consistency and availability in a distributed ride-matching system. Discussion of event-driven architecture for trip state changes and CQRS for separating read/write paths demonstrates system maturity.