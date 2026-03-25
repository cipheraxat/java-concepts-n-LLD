# Amazon Locker

**Difficulty:** Easy | **Companies:** Amazon

---

## Requirements

As the interview begins, we'll likely be greeted by a simple prompt to set the stage for the architecture we need to design.

> "Design the object-oriented backend for an Amazon Locker system. Customers receive an access code when a package is assigned to a locker compartment and enter the code to retrieve it."

Before jumping into class design, you should ask questions of your interviewer. The goal here is to turn that vague prompt into a concrete specification — something you can actually build against.

### Clarifying Questions

The goal is to surface ambiguity early and get to a concrete spec. A reliable way to structure your questions is to cover four areas: what the core actions are, how errors should be handled, what the boundaries of the system are, and whether we need to plan for future extensions.

> **You:** "Are we dealing with a single locker location or multiple?"
>
> **Interviewer:** "Multiple. The system manages several locker locations, each with its own set of compartments."

Good. Now figure out the physical layout details.

> **You:** "What sizes do compartments come in?"
>
> **Interviewer:** "Small, Medium, and Large. A package must be assigned to a compartment that matches its size."

> **You:** "How does a customer pick up their package?"
>
> **Interviewer:** "The system generates a unique 6-digit access code when a package is assigned. The customer enters the code at the locker to open the compartment."

Now think about what happens when things go wrong or time out.

> **You:** "Is there a time limit for pickup?"
>
> **Interviewer:** "Yes. Packages not picked up within 3 days expire and should be flagged."

> **You:** "What if there's no compartment available at the preferred location?"
>
> **Interviewer:** "Try another location. If no location has space, the assignment fails."

Finally, check for scope boundaries.

> **You:** "Do we need to handle returns or package delivery logistics?"
>
> **Interviewer:** "No. Just assignment, retrieval, and expiry."

### Final Requirements

```
Requirements:
1. Multiple locker locations, each with compartments of sizes Small, Medium, Large
2. A package is assigned to a compartment at a preferred location (fallback to any available)
3. A unique 6-digit access code is generated at time of assignment
4. Customer enters the access code to retrieve their package; compartment is released
5. Packages not picked up within 3 days are flagged as expired

Out of scope:
- Package delivery logistics
- Returns
- UI / notification systems
- Payment or billing
```

---

## Core Entities and Relationships

Start by asking: what are the main "things" in this problem? Look for nouns in your requirements. In Amazon Locker, a few jump out: the locker system itself, the physical locations, compartments, packages, and access codes.

| Entity | Responsibility |
|--------|---------------|
| **LockerSystem** | Singleton facade over all locations. Assigns packages to compartments, handles retrieval by code, and checks for expired packages. External code only interacts through this class. |
| **LockerLocation** | A physical locker bank at an address. Groups compartments so location-level queries ("does this location have space?") are answered without touching system-level routing logic. |
| **Compartment** | A single storage slot with a size and occupancy state. The smallest lockable resource unit — owns its own state transitions (occupy/release). |
| **Package** | The item to store. Carries a size requirement that drives compartment selection. Decoupled from Compartment so a package can be re-assigned if needed. |
| **AccessCode** | Links a one-time code to a specific package and compartment. Tracks the expiry timestamp. Security credential separated from both Package and Compartment. |

---

## Class Design

Now that we've identified the five core entities, the next step is defining their interfaces. Start with a top-down approach — begin with LockerSystem since it's the orchestrator and primary entry point.

### LockerSystem

The LockerSystem class is the facade. External code assigns packages and retrieves them only through this class.

| Requirement | What LockerSystem must track |
|-------------|------------------------------|
| "Multiple locker locations" | A list of LockerLocation objects |
| "Package is assigned to a compartment" | Map from packageId → AccessCode (guards against double-assignment) |
| "Customer enters access code to retrieve" | Map from code → AccessCode (O(1) reverse lookup) |
| "Compartment is released on retrieval" | Map from packageId → LockerLocation (to release the right compartment) |

```
class LockerSystem:
    - locations: List<LockerLocation>
    - packageToCode: Map<String, AccessCode>
    - codeToAccess: Map<String, AccessCode>
    - packageToLocation: Map<String, LockerLocation>
```

Next, look at the actions:

| Need from requirements | Method on LockerSystem |
|------------------------|------------------------|
| "Assign package to a compartment" | assignPackage(package, preferredLocationId) → String? |
| "Customer enters code to retrieve" | retrievePackage(code) → RetrievalStatus |
| "Packages not picked up expire" | checkExpiredPackages() → List<String> |

```
class LockerSystem:
    - locations: List<LockerLocation>
    - packageToCode: Map<String, AccessCode>
    - codeToAccess: Map<String, AccessCode>
    - packageToLocation: Map<String, LockerLocation>

    + LockerSystem(locations)
    + assignPackage(package, preferredLocationId) → String?
    + retrievePackage(code) → RetrievalStatus
    + checkExpiredPackages() → List<String>
```

### LockerLocation

LockerLocation groups compartments at a physical address. It needs to find available compartments by size.

| Requirement | What LockerLocation must track |
|-------------|-------------------------------|
| "Each location has compartments of different sizes" | A list of Compartment objects |
| "Assign to a compartment that matches size" | Ability to find and occupy a matching compartment |

```
class LockerLocation:
    - locationId: String
    - address: String
    - compartments: List<Compartment>

    + assignCompartment(package) → Compartment?
    + releaseCompartment(compartmentId) → void
    + hasAvailableSpace(size) → boolean
```

### Compartment

| Requirement | What Compartment must track |
|-------------|----------------------------|
| "Sizes: Small, Medium, Large" | A size enum |
| "Compartment is occupied or available" | An occupancy state and the current packageId |

```
class Compartment:
    - compartmentId: String
    - size: Size
    - status: CompartmentStatus
    - currentPackageId: String?

    + isAvailable() → boolean
    + occupy(packageId) → void
    + release() → void
```

### Package

| Requirement | What Package must track |
|-------------|------------------------|
| "Package must match compartment size" | A required size |
| "Assigned per customer" | A packageId and customerId |

```
class Package:
    - packageId: String
    - customerId: String
    - requiredSize: Size
```

### AccessCode

| Requirement | What AccessCode must track |
|-------------|---------------------------|
| "6-digit access code" | The code string |
| "Links code to package and compartment" | packageId, compartmentId |
| "3-day expiry" | An expiry timestamp |

```
class AccessCode:
    - code: String
    - packageId: String
    - compartmentId: String
    - expiresAt: LocalDateTime

    + isExpired() → boolean
```

### Final Class Design

```
class LockerSystem:
    - locations: List<LockerLocation>
    - packageToCode: Map<String, AccessCode>
    - codeToAccess: Map<String, AccessCode>
    - packageToLocation: Map<String, LockerLocation>

    + LockerSystem(locations)
    + assignPackage(package, preferredLocationId) → String?
    + retrievePackage(code) → RetrievalStatus
    + checkExpiredPackages() → List<String>

class LockerLocation:
    - locationId: String
    - address: String
    - compartments: List<Compartment>

    + assignCompartment(package) → Compartment?
    + releaseCompartment(compartmentId) → void
    + hasAvailableSpace(size) → boolean

class Compartment:
    - compartmentId: String
    - size: Size
    - status: CompartmentStatus
    - currentPackageId: String?

    + isAvailable() → boolean
    + occupy(packageId) → void
    + release() → void

class Package:
    - packageId: String
    - customerId: String
    - requiredSize: Size

class AccessCode:
    - code: String
    - packageId: String
    - compartmentId: String
    - expiresAt: LocalDateTime

    + isExpired() → boolean

enum Size:
    SMALL, MEDIUM, LARGE

enum CompartmentStatus:
    AVAILABLE, OCCUPIED

enum RetrievalStatus:
    SUCCESS, INVALID_CODE, EXPIRED
```

---

## Implementation

For each method, follow a consistent pattern: define the core logic (happy path), then consider edge cases.

### LockerSystem.assignPackage

**Core logic:**
1. Check if the package is already assigned (guard against double-assignment)
2. Try the preferred location first
3. If no space there, try any other location
4. Ask the location to assign a compartment
5. Generate a 6-digit code, create an AccessCode, update all three maps

**Edge cases:**
- Package already assigned → return null
- No location has space → return null

```
assignPackage(pkg, preferredLocationId)
    if packageToCode.contains(pkg.packageId)
        return null   // already assigned

    target = findLocation(preferredLocationId) if it has space for pkg.size
             else findAnyAvailableLocation(pkg.size)

    if target is null
        return null   // no space anywhere

    compartment = target.assignCompartment(pkg)
    if compartment is null
        return null

    code = generateRandomCode(6 digits)
    accessCode = new AccessCode(code, pkg.packageId, compartment.id, now + 3 days)

    packageToCode[pkg.packageId] = accessCode
    codeToAccess[code] = accessCode
    packageToLocation[pkg.packageId] = target

    return code
```

### LockerSystem.retrievePackage

**Core logic:**
1. Look up the access code
2. Check if expired
3. Release the compartment
4. Clean up all three maps

**Edge cases:**
- Code not found → INVALID_CODE
- Code expired → EXPIRED

```
retrievePackage(code)
    accessCode = codeToAccess[code]
    if accessCode is null
        return INVALID_CODE
    if accessCode.isExpired()
        return EXPIRED

    location = packageToLocation[accessCode.packageId]
    location.releaseCompartment(accessCode.compartmentId)

    codeToAccess.remove(code)
    packageToCode.remove(accessCode.packageId)
    packageToLocation.remove(accessCode.packageId)

    return SUCCESS
```

### LockerLocation.assignCompartment

**Core logic:** Find the first available compartment matching the exact size, mark it occupied.

```
assignCompartment(pkg)
    for each compartment in compartments:
        if compartment.isAvailable() and compartment.size == pkg.requiredSize
            compartment.occupy(pkg.packageId)
            return compartment
    return null
```

### Complete Code Implementation

```java
public enum Size { SMALL, MEDIUM, LARGE }

public enum CompartmentStatus { AVAILABLE, OCCUPIED }

public enum RetrievalStatus { SUCCESS, INVALID_CODE, EXPIRED }
```

```java
public class Package {
    private final String packageId;
    private final String customerId;
    private final Size requiredSize;

    public Package(String packageId, String customerId, Size requiredSize) {
        this.packageId = packageId;
        this.customerId = customerId;
        this.requiredSize = requiredSize;
    }

    public String getPackageId()   { return packageId; }
    public String getCustomerId()  { return customerId; }
    public Size getRequiredSize()  { return requiredSize; }
}
```

```java
import java.time.LocalDateTime;

public class AccessCode {
    private final String code;
    private final String packageId;
    private final String compartmentId;
    private final LocalDateTime expiresAt;

    public AccessCode(String code, String packageId, String compartmentId,
                      LocalDateTime expiresAt) {
        this.code = code;
        this.packageId = packageId;
        this.compartmentId = compartmentId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String getCode()          { return code; }
    public String getPackageId()     { return packageId; }
    public String getCompartmentId() { return compartmentId; }
}
```

```java
public class Compartment {
    private final String compartmentId;
    private final Size size;
    private CompartmentStatus status;
    private String currentPackageId;

    public Compartment(String compartmentId, Size size) {
        this.compartmentId = compartmentId;
        this.size = size;
        this.status = CompartmentStatus.AVAILABLE;
    }

    public boolean isAvailable()       { return status == CompartmentStatus.AVAILABLE; }
    public Size getSize()              { return size; }
    public String getCompartmentId()   { return compartmentId; }

    public void occupy(String packageId) {
        this.status = CompartmentStatus.OCCUPIED;
        this.currentPackageId = packageId;
    }

    public void release() {
        this.status = CompartmentStatus.AVAILABLE;
        this.currentPackageId = null;
    }
}
```

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LockerLocation {
    private final String locationId;
    private final String address;
    private final List<Compartment> compartments;

    public LockerLocation(String locationId, String address,
                          List<Compartment> compartments) {
        this.locationId = locationId;
        this.address = address;
        this.compartments = new ArrayList<>(compartments);
    }

    public Optional<Compartment> assignCompartment(Package pkg) {
        return compartments.stream()
            .filter(c -> c.isAvailable() && c.getSize() == pkg.getRequiredSize())
            .findFirst()
            .map(c -> { c.occupy(pkg.getPackageId()); return c; });
    }

    public void releaseCompartment(String compartmentId) {
        compartments.stream()
            .filter(c -> c.getCompartmentId().equals(compartmentId))
            .findFirst()
            .ifPresent(Compartment::release);
    }

    public boolean hasAvailableSpace(Size size) {
        return compartments.stream()
            .anyMatch(c -> c.isAvailable() && c.getSize() == size);
    }

    public String getLocationId() { return locationId; }
    public String getAddress()    { return address; }
}
```

```java
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LockerSystem {
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_DAYS = 3;

    private final List<LockerLocation> locations;
    private final Map<String, AccessCode> packageToCode = new HashMap<>();
    private final Map<String, AccessCode> codeToAccess = new HashMap<>();
    private final Map<String, LockerLocation> packageToLocation = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public LockerSystem(List<LockerLocation> locations) {
        this.locations = locations;
    }

    public Optional<String> assignPackage(Package pkg, String preferredLocationId) {
        if (packageToCode.containsKey(pkg.getPackageId())) {
            return Optional.empty();
        }

        LockerLocation target = findLocation(preferredLocationId)
            .filter(loc -> loc.hasAvailableSpace(pkg.getRequiredSize()))
            .orElseGet(() -> findAnyAvailableLocation(pkg).orElse(null));

        if (target == null) return Optional.empty();

        Optional<Compartment> compartment = target.assignCompartment(pkg);
        if (compartment.isEmpty()) return Optional.empty();

        String code = generateCode();
        AccessCode accessCode = new AccessCode(
            code, pkg.getPackageId(),
            compartment.get().getCompartmentId(),
            LocalDateTime.now().plusDays(EXPIRY_DAYS)
        );

        packageToCode.put(pkg.getPackageId(), accessCode);
        codeToAccess.put(code, accessCode);
        packageToLocation.put(pkg.getPackageId(), target);

        return Optional.of(code);
    }

    public RetrievalStatus retrievePackage(String code) {
        AccessCode accessCode = codeToAccess.get(code);
        if (accessCode == null) return RetrievalStatus.INVALID_CODE;
        if (accessCode.isExpired()) return RetrievalStatus.EXPIRED;

        LockerLocation location = packageToLocation.get(accessCode.getPackageId());
        if (location != null) {
            location.releaseCompartment(accessCode.getCompartmentId());
        }

        codeToAccess.remove(code);
        packageToCode.remove(accessCode.getPackageId());
        packageToLocation.remove(accessCode.getPackageId());

        return RetrievalStatus.SUCCESS;
    }

    public List<String> checkExpiredPackages() {
        List<String> expired = new ArrayList<>();
        for (var entry : packageToCode.entrySet()) {
            if (entry.getValue().isExpired()) {
                expired.add(entry.getKey());
            }
        }
        return expired;
    }

    private Optional<LockerLocation> findLocation(String locationId) {
        return locations.stream()
            .filter(l -> l.getLocationId().equals(locationId))
            .findFirst();
    }

    private Optional<LockerLocation> findAnyAvailableLocation(Package pkg) {
        return locations.stream()
            .filter(l -> l.hasAvailableSpace(pkg.getRequiredSize()))
            .findFirst();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
```

### Verification

```
Setup: Location "LOC-1" at "123 Main St" with compartments:
  L1-S1 (SMALL), L1-S2 (SMALL), L1-M1 (MEDIUM), L1-L1 (LARGE)

Step 1: Assign PKG-001 (SMALL) to preferred "LOC-1"
  packageToCode empty → not a duplicate
  LOC-1 has available SMALL → target = LOC-1
  assignCompartment → L1-S1 matches (SMALL, AVAILABLE) → occupy → L1-S1
  generateCode → "482910"
  Maps updated: packageToCode[PKG-001], codeToAccess[482910], packageToLocation[PKG-001]
  Return "482910"

Step 2: Retrieve with code "482910"
  codeToAccess["482910"] → found AccessCode
  isExpired()? No (within 3 days)
  LOC-1.releaseCompartment("L1-S1") → L1-S1 status = AVAILABLE
  Remove from all three maps
  Return SUCCESS

Step 3: Retrieve again with code "482910"
  codeToAccess["482910"] → null
  Return INVALID_CODE

Step 4: Assign PKG-002 (SMALL) to "LOC-1"
  L1-S1 is now AVAILABLE again → assigned to L1-S1
  New code generated
```

This verifies assignment, retrieval, compartment release and reuse, and invalid code rejection.

---

## Extensibility

### 1. "What if compartment fitting should be flexible — a MEDIUM compartment can hold a SMALL package?"

> "Today packages must match exact compartment size because that's the requirement. To support flexible fitting, I'd add an ordering to the Size enum so we can compare sizes. The compartment search would filter on `compartment.size >= package.requiredSize` instead of exact match. I'd also sort matches by size ascending to prefer the smallest fitting compartment and avoid wasting large compartments on small packages."

```
enum Size:
    SMALL(1), MEDIUM(2), LARGE(3)

    + canFit(required: Size) → boolean
        return this.level >= required.level

// In LockerLocation.assignCompartment:
compartments.filter(c -> c.isAvailable() && c.size.canFit(pkg.requiredSize))
            .sortBy(c -> c.size.level)
            .findFirst()
```

### 2. "How would you notify customers when their package is about to expire?"

> "Notification belongs outside the core locker logic. I'd introduce an `ExpiryListener` interface with an `onPackageExpiring(packageId, customerId)` callback. A scheduled task runs `checkExpiredPackages` periodically and calls the listener for packages nearing expiry. The concrete listener could send an email, SMS, or push notification — LockerSystem doesn't need to know."

```
interface ExpiryListener:
    + onPackageExpiring(packageId, customerId)

// In LockerSystem — scheduled check:
checkAndNotify(listener)
    for each entry in packageToCode:
        if entry.value is within 24 hours of expiry
            listener.onPackageExpiring(entry.key, ...)
```

### 3. "What if we need to find the nearest locker by GPS coordinates?"

> "I'd add latitude and longitude fields to LockerLocation and a `distanceTo(lat, lon)` method using the Haversine formula. In LockerSystem, instead of trying the preferred location first, the `assignPackage` method would accept coordinates and sort available locations by distance, then assign to the nearest one with space."

```
class LockerLocation:
    - latitude: double
    - longitude: double

    + distanceTo(lat, lon) → double
        // Haversine formula

// In LockerSystem:
assignPackageByProximity(pkg, lat, lon) → String?
    target = locations.filter(l -> l.hasAvailableSpace(pkg.size))
                      .sortBy(l -> l.distanceTo(lat, lon))
                      .findFirst()
```

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can decompose the problem into logical pieces and implement a working assignment and retrieval flow. You should identify that you need compartments with sizes, some way to generate a code, and a lookup to retrieve packages. Your code should handle basic assignment and retrieval. It's fine if the code generation is simple or if you don't think about expiry initially. If your system correctly assigns a package, returns a code, and releases the compartment on retrieval, you're doing well.

### Mid-level

For mid-level candidates, I expect cleaner separation — LockerSystem as a facade, LockerLocation grouping compartments, and AccessCode as its own entity with expiry logic. Your maps should support O(1) lookups in both directions (package-to-code and code-to-access). You should handle edge cases: double-assignment, invalid codes, and expired packages. I'd expect you to discuss at least one extensibility point and explain where changes would go.

### Senior

Senior candidates should produce a design with clear justification for each class boundary. You'd explain why AccessCode is separate from both Package and Compartment, why LockerSystem uses three maps, and proactively discuss thread safety (concurrent deliveries). Extensibility discussions should cover flexible size fitting with tradeoffs (wasted space vs. availability) and the observer pattern for notifications. You might also discuss the SecureRandom choice for code generation to prevent guessable codes.
