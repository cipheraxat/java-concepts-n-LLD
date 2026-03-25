# Rate Limiter

**Difficulty:** Medium | **Companies:** Google, Stripe, Cloudflare, Amazon

---

## Requirements

> "Design a rate limiter that controls how many requests a user can make within a time window. It should support multiple rate-limiting algorithms via a strategy pattern."

### Clarifying Questions

> **You:** "Per-user or global rate limiting?"
>
> **Interviewer:** "Per-user. Each user has their own rate limit tracked independently."

> **You:** "Which algorithms should we support?"
>
> **Interviewer:** "At minimum: Token Bucket and Sliding Window Log. Make it pluggable so new algorithms can be added."

> **You:** "Is this in-process or distributed?"
>
> **Interviewer:** "In-process. Single JVM. But design for thread safety."

> **You:** "What happens when a request is throttled?"
>
> **Interviewer:** "Return a result indicating allowed or denied, with time until next allowed request."

### Final Requirements

```
Requirements:
1. Per-user rate limiting with configurable limits
2. Strategy pattern for pluggable algorithms (Token Bucket, Sliding Window Log)
3. isAllowed(userId) → RateLimitResult (allowed/denied + retryAfter)
4. Thread-safe for concurrent access
5. Automatic cleanup of stale user entries

Out of scope:
- Distributed rate limiting (Redis-based)
- IP-based or endpoint-based limiting
- HTTP integration / middleware
- Persistent storage
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **RateLimiter** | Facade. Routes requests to the configured strategy. Manages per-user state via ConcurrentHashMap. |
| **RateLimitStrategy** | Interface defining the algorithm contract. |
| **TokenBucketStrategy** | Refills tokens at a steady rate. Each request consumes a token. |
| **SlidingWindowLogStrategy** | Tracks exact timestamps of requests within the window. Counts entries in the sliding window. |
| **RateLimitResult** | Value object: allowed (boolean), retryAfterMillis (long). |

---

## Class Design

### RateLimiter

| Requirement | What RateLimiter must track |
|-------------|----------------------------|
| "Per-user" | ConcurrentHashMap: userId → strategy-specific state |
| "Pluggable algorithm" | Hold a RateLimitStrategy reference |

```
class RateLimiter:
    - strategy: RateLimitStrategy

    + isAllowed(userId) → RateLimitResult
```

### Token Bucket

```
class TokenBucketStrategy implements RateLimitStrategy:
    - maxTokens: int
    - refillRate: double  // tokens per second
    - buckets: ConcurrentHashMap<String, TokenBucket>

    + isAllowed(userId) → RateLimitResult

class TokenBucket:
    - tokens: double
    - lastRefillTime: long (nanos)
    - lock: ReentrantLock

    + tryConsume() → RateLimitResult
```

### Sliding Window Log

```
class SlidingWindowLogStrategy implements RateLimitStrategy:
    - maxRequests: int
    - windowMillis: long
    - logs: ConcurrentHashMap<String, Deque<Long>>

    + isAllowed(userId) → RateLimitResult
```

### Final Class Design

```
interface RateLimitStrategy:
    + isAllowed(userId) → RateLimitResult

class RateLimiter:
    - strategy: RateLimitStrategy
    + isAllowed(userId) → RateLimitResult

class TokenBucketStrategy:
    - maxTokens, refillRate
    - buckets: ConcurrentHashMap<String, TokenBucket>

class SlidingWindowLogStrategy:
    - maxRequests, windowMillis
    - logs: ConcurrentHashMap<String, Deque<Long>>

class RateLimitResult:
    - allowed: boolean
    - retryAfterMillis: long
```

---

## Implementation

### TokenBucket.tryConsume

**Core logic:** Refill tokens based on elapsed time, then try to consume one.

```
tryConsume()
    lock.lock()
    try:
        now = nanoTime()
        elapsed = (now - lastRefillTime) / 1e9
        tokens = min(maxTokens, tokens + elapsed × refillRate)
        lastRefillTime = now

        if tokens >= 1.0:
            tokens -= 1.0
            return RateLimitResult(allowed=true, retryAfter=0)
        else:
            deficit = 1.0 - tokens
            waitSeconds = deficit / refillRate
            return RateLimitResult(allowed=false, retryAfter=waitSeconds×1000)
    finally:
        lock.unlock()
```

### SlidingWindowLogStrategy.isAllowed

**Core logic:** Remove timestamps outside the window, count remaining. If under limit, add current timestamp.

```
isAllowed(userId)
    now = currentTimeMillis()
    log = logs.computeIfAbsent(userId, → new ArrayDeque)
    synchronized(log):
        cutoff = now - windowMillis
        while log is not empty and log.peekFirst() <= cutoff:
            log.pollFirst()
        if log.size() < maxRequests:
            log.addLast(now)
            return RateLimitResult(allowed=true, retryAfter=0)
        else:
            oldest = log.peekFirst()
            retryAfter = oldest + windowMillis - now
            return RateLimitResult(allowed=false, retryAfter=retryAfter)
```

### Complete Code Implementation

```java
public class RateLimitResult {
    private final boolean allowed;
    private final long retryAfterMillis;

    public RateLimitResult(boolean allowed, long retryAfterMillis) {
        this.allowed = allowed;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isAllowed()       { return allowed; }
    public long getRetryAfterMillis() { return retryAfterMillis; }

    @Override
    public String toString() {
        return allowed ? "ALLOWED" : "DENIED (retry after " + retryAfterMillis + "ms)";
    }
}
```

```java
public interface RateLimitStrategy {
    RateLimitResult isAllowed(String userId);
}
```

```java
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {
    private double tokens;
    private long lastRefillNanos;
    private final int maxTokens;
    private final double refillRate;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(int maxTokens, double refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
        this.tokens = maxTokens;
        this.lastRefillNanos = System.nanoTime();
    }

    public RateLimitResult tryConsume() {
        lock.lock();
        try {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(maxTokens, tokens + elapsedSeconds * refillRate);
            lastRefillNanos = now;

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return new RateLimitResult(true, 0);
            } else {
                double deficit = 1.0 - tokens;
                long waitMillis = (long) Math.ceil((deficit / refillRate) * 1000);
                return new RateLimitResult(false, waitMillis);
            }
        } finally {
            lock.unlock();
        }
    }
}
```

```java
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketStrategy implements RateLimitStrategy {
    private final int maxTokens;
    private final double refillRate;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketStrategy(int maxTokens, double refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
    }

    @Override
    public RateLimitResult isAllowed(String userId) {
        TokenBucket bucket = buckets.computeIfAbsent(userId,
            k -> new TokenBucket(maxTokens, refillRate));
        return bucket.tryConsume();
    }
}
```

```java
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowLogStrategy implements RateLimitStrategy {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Deque<Long>> logs = new ConcurrentHashMap<>();

    public SlidingWindowLogStrategy(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    @Override
    public RateLimitResult isAllowed(String userId) {
        long now = System.currentTimeMillis();
        Deque<Long> log = logs.computeIfAbsent(userId, k -> new ArrayDeque<>());

        synchronized (log) {
            long cutoff = now - windowMillis;
            while (!log.isEmpty() && log.peekFirst() <= cutoff) {
                log.pollFirst();
            }

            if (log.size() < maxRequests) {
                log.addLast(now);
                return new RateLimitResult(true, 0);
            } else {
                long oldest = log.peekFirst();
                long retryAfter = oldest + windowMillis - now;
                return new RateLimitResult(false, Math.max(retryAfter, 1));
            }
        }
    }
}
```

```java
public class RateLimiter {
    private final RateLimitStrategy strategy;

    public RateLimiter(RateLimitStrategy strategy) {
        this.strategy = strategy;
    }

    public RateLimitResult isAllowed(String userId) {
        return strategy.isAllowed(userId);
    }
}
```

### Verification

```
Setup: TokenBucketStrategy(maxTokens=3, refillRate=1.0/sec)
User: Alice

Step 1: t=0s — Alice request #1
  bucket created: tokens=3.0
  tokens=3.0, elapsed=0 → consume → tokens=2.0 → ALLOWED

Step 2: t=0s — Alice request #2
  elapsed≈0, tokens=2.0 → consume → tokens=1.0 → ALLOWED

Step 3: t=0s — Alice request #3
  tokens=1.0 → consume → tokens=0.0 → ALLOWED

Step 4: t=0s — Alice request #4
  tokens=0.0, deficit=1.0, wait=1.0/1.0=1000ms → DENIED (retry after 1000ms)

Step 5: t=1.5s — Alice request #5
  elapsed=1.5s, refill=1.5×1.0=1.5, tokens=min(3, 0+1.5)=1.5
  consume → tokens=0.5 → ALLOWED

---
Setup: SlidingWindowLogStrategy(maxRequests=2, windowMillis=60000)
User: Bob

Step 1: t=0ms — Bob request #1
  log=[], size=0 < 2 → add(0) → ALLOWED

Step 2: t=100ms — Bob request #2
  log=[0], size=1 < 2 → add(100) → ALLOWED

Step 3: t=200ms — Bob request #3
  log=[0, 100], size=2 ≥ 2
  retryAfter = 0 + 60000 - 200 = 59800ms → DENIED

Step 4: t=60001ms — Bob request #4
  cutoff=60001-60000=1 → purge timestamps ≤ 1 → remove 0
  log=[100], size=1 < 2 → add(60001) → ALLOWED
```

---

## Extensibility

### 1. "How would you add a Fixed Window Counter?"

> "I'd create a `FixedWindowStrategy` that divides time into fixed windows (e.g., 1-minute buckets). Each user has an `AtomicInteger` counter and a window start time. If the current time is still in the same window, increment the counter. If the window has rolled over, reset. The boundary spike problem is a known trade-off — you can mention Sliding Window Counter as a hybrid."

### 2. "How would you make this distributed?"

> "I'd replace the in-memory ConcurrentHashMap with Redis. For Token Bucket, a Lua script atomically reads the token count, calculates refill, decrements, and writes back. For Sliding Window Log, a Redis sorted set with timestamps as scores works — `ZRANGEBYSCORE` removes old entries and `ZCARD` checks the count."

### 3. "How would you add per-endpoint rate limits?"

> "I'd change the key from just `userId` to a composite key like `userId:endpoint`. The RateLimiter accepts a `RateLimitKey` object. Different endpoints can have different limits by configuring separate strategies in a registry: `Map<String, RateLimitStrategy>`."

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking that you understand the Token Bucket algorithm conceptually — tokens refill over time, each request costs a token, requests are denied when the bucket is empty. A basic implementation with an if-check on token count is fine. Thread safety isn't required.

### Mid-level

Mid-level candidates should implement both Token Bucket and Sliding Window Log cleanly, using the Strategy pattern so they're interchangeable. Per-user state should use ConcurrentHashMap. The sliding window should correctly purge old timestamps. You'd discuss the trade-offs: Token Bucket allows bursts, Sliding Window Log uses more memory.

### Senior

Senior candidates would discuss the precision vs. memory trade-off between algorithms, handle edge cases like clock drift for the sliding window, and explain how to move to a distributed architecture (Redis + Lua scripts). You'd mention the Fixed Window boundary problem and how Sliding Window Counter solves it. Thread safety should be tight — lock per bucket, not global.