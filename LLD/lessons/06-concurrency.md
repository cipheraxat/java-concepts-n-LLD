# Lesson 6: Concurrency

---

## The Three Concurrency Problem Patterns

All concurrency problems in interviews fall into exactly three categories. Recognising which one you're facing tells you immediately which primitive to reach for.

---

### 1. Correctness Issues *(3:27)*

Occur when **shared state is corrupted by simultaneous access**. The two main sub-patterns to watch for:

- **Check-then-act** — e.g., checking a seat is available and then booking it; another thread can sneak in between the check and the act.
- **Read-modify-write** — e.g., incrementing a counter (`count++` is actually read → add 1 → write back); two threads can both read the old value and both write the same incremented result, losing one update.

**Solution:** Use **locks** or **atomic variables** to ensure the entire operation is atomic *(5:26)*.

---

### 2. Coordination Issues *(9:54)*

Involve **managing the flow of tasks between different threads** — e.g., a producer generating work items and passing them to a background consumer thread. Polling in a busy-loop wastes CPU and is error-prone.

**Solution:** Use **blocking queues** to handle producer-consumer scenarios efficiently without wasting CPU cycles *(12:52)*. The queue itself handles all the signalling — the producer `put()`s and the consumer `take()`s, blocking automatically when the queue is full or empty.

---

### 3. Scarcity Issues *(16:01)*

Arise when **managing a finite number of resources** — e.g., limiting the number of concurrent API calls to an external service, or managing a fixed-size database connection pool.

**Solution:** Use **semaphores** to count permits, or **bounded blocking queues** to pool finite resources, ensuring they are always properly released *(16:27)*.

> By understanding these three patterns and their associated primitives, you can predictably solve complex concurrency problems in any interview.

---

## Core Java Concurrency Tools

| Tool | Package | When to Use |
|------|---------|-----------|
| `synchronized` | Built-in | Simple critical sections |
| `ReentrantLock` | `java.util.concurrent.locks` | Try-lock, timed lock, fair ordering |
| `ReadWriteLock` | `java.util.concurrent.locks` | Many readers, few writers |
| `AtomicInteger` / `AtomicLong` | `java.util.concurrent.atomic` | Single variable lock-free counter |
| `AtomicReference<T>` | `java.util.concurrent.atomic` | Lock-free CAS on objects |
| `ConcurrentHashMap` | `java.util.concurrent` | Thread-safe map |
| `BlockingQueue` | `java.util.concurrent` | Producer-consumer pattern |
| `Semaphore` | `java.util.concurrent` | Limit concurrent access to N |
| `CountDownLatch` | `java.util.concurrent` | Wait for N events to complete |
| `ExecutorService` | `java.util.concurrent` | Thread pool management |

---

## 1. `synchronized` — Basic Mutual Exclusion

```java
public class BankAccount {
    private double balance;

    // Synchronized method — only one thread at a time
    public synchronized void deposit(double amount) {
        balance += amount;
    }

    public synchronized boolean withdraw(double amount) {
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }

    public synchronized double getBalance() {
        return balance;
    }
}

// Synchronized block — more granular locking
public class Counter {
    private int count = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized (lock) {
            count++;
        }
    }

    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }
}
```

---

## 2. `ReentrantLock` — Explicit Lock with More Control

```java
import java.util.concurrent.locks.ReentrantLock;

public class ParkingSpot {
    private final ReentrantLock lock = new ReentrantLock();
    private boolean occupied = false;

    // Try to occupy — non-blocking attempt
    public boolean tryOccupy() {
        if (lock.tryLock()) {
            try {
                if (!occupied) {
                    occupied = true;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();  // ALWAYS unlock in finally
            }
        }
        return false;  // another thread holds the lock
    }

    public void vacate() {
        lock.lock();
        try {
            occupied = false;
        } finally {
            lock.unlock();
        }
    }
}
```

---

## 3. `ReadWriteLock` — Concurrent Readers, Exclusive Writers

Use when reads are frequent and writes are rare.

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<K, V> {
    private final Map<K, V> data = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public V get(K key) {
        rwLock.readLock().lock();       // multiple readers allowed concurrently
        try {
            return data.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        rwLock.writeLock().lock();      // exclusive — no readers or other writers
        try {
            data.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void remove(K key) {
        rwLock.writeLock().lock();
        try {
            data.remove(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

---

## 4. Atomic Variables — Lock-Free Operations

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RequestCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicLong totalLatencyNs = new AtomicLong(0);

    public void record(long latencyNs) {
        count.incrementAndGet();                // atomic increment
        totalLatencyNs.addAndGet(latencyNs);    // atomic add
    }

    public double averageLatencyMs() {
        int c = count.get();
        return c == 0 ? 0 : (totalLatencyNs.get() / 1_000_000.0) / c;
    }
}

// Compare-and-Swap (CAS) — optimistic locking
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {
    private static class Node<T> {
        final T value;
        final Node<T> next;

        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>(null);

    public void push(T value) {
        while (true) {
            Node<T> oldHead = head.get();
            Node<T> newHead = new Node<>(value, oldHead);
            if (head.compareAndSet(oldHead, newHead)) return;  // retry if someone else pushed
        }
    }

    public Optional<T> pop() {
        while (true) {
            Node<T> oldHead = head.get();
            if (oldHead == null) return Optional.empty();
            if (head.compareAndSet(oldHead, oldHead.next)) return Optional.of(oldHead.value);
        }
    }
}
```

---

## 5. Producer-Consumer with `BlockingQueue`

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Producer
public class TaskProducer implements Runnable {
    private final BlockingQueue<String> queue;

    public TaskProducer(BlockingQueue<String> queue) { this.queue = queue; }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 10; i++) {
                queue.put("Task-" + i);    // blocks if queue is full
                System.out.println("Produced: Task-" + i);
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Consumer
public class TaskConsumer implements Runnable {
    private final BlockingQueue<String> queue;

    public TaskConsumer(BlockingQueue<String> queue) { this.queue = queue; }

    @Override
    public void run() {
        try {
            while (true) {
                String task = queue.take();    // blocks if queue is empty
                System.out.println("Consumed: " + task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Main
BlockingQueue<String> queue = new LinkedBlockingQueue<>(5);   // capacity 5 → backpressure
Thread producer = new Thread(new TaskProducer(queue));
Thread consumer = new Thread(new TaskConsumer(queue));
producer.start();
consumer.start();
```

---

## 6. Thread Pools with `ExecutorService`

```java
import java.util.concurrent.*;

public class TaskSchedulerExample {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Submit a one-time task
    public Future<String> processAsync(String data) {
        return executor.submit(() -> {
            // do work
            return "Processed: " + data;
        });
    }

    // Schedule a recurring task
    public void scheduleHealthCheck() {
        scheduler.scheduleAtFixedRate(
            () -> System.out.println("Health check at " + System.currentTimeMillis()),
            0,          // initial delay
            30,         // period
            TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        executor.shutdown();
        scheduler.shutdown();
    }
}
```

---

## 7. Semaphore — Limit Concurrent Access

```java
import java.util.concurrent.Semaphore;

// Limit concurrent database connections to 10
public class ConnectionPool {
    private final Semaphore semaphore;
    private final Queue<Connection> connections;

    public ConnectionPool(int maxConnections) {
        this.semaphore = new Semaphore(maxConnections);
        this.connections = new LinkedList<>();
        // initialize connections...
    }

    public Connection acquire() throws InterruptedException {
        semaphore.acquire();    // blocks if all connections are in use
        synchronized (connections) {
            return connections.poll();
        }
    }

    public void release(Connection conn) {
        synchronized (connections) {
            connections.offer(conn);
        }
        semaphore.release();    // signal one connection is back
    }
}
```

---

## 8. Preventing Deadlocks

**Deadlock occurs when:** Thread A holds lock X, waits for lock Y. Thread B holds lock Y, waits for lock X.

**Prevention:** Always acquire locks in the **same global order**.

```java
// Deadlock-prone: locks acquired in different orders by different callers
public class Transfer {
    public void transfer(Account from, Account to, double amount) {
        synchronized (from) {       // Thread A: locks "from" first
            synchronized (to) {     // Thread B: locks "to" first (different order!)
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}

// Deadlock-safe: enforce consistent lock ordering by account ID
public class SafeTransfer {
    public void transfer(Account from, Account to, double amount) {
        Account first  = from.getId().compareTo(to.getId()) < 0 ? from : to;
        Account second = first == from ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

---

## 9. `volatile` — Visibility Guarantee

```java
public class ConfigWatcher {
    // Without volatile: changes in one thread may not be visible to others
    private volatile boolean running = true;   // volatile ensures visibility

    public void start() {
        while (running) {
            // poll for changes
        }
    }

    public void stop() {
        running = false;    // guaranteed to be visible to all threads immediately
    }
}
```

---

## Concurrency in LLD Problems

| Problem | Concurrency Concern | Solution |
|---------|-------------------|---------|
| **Parking Lot** | Multiple cars entering simultaneously → double-assign spot | `synchronized` on spot assignment |
| **Booking System** | Two users book same seat → double booking | `synchronized` check + hold block |
| **Rate Limiter** | Concurrent requests → counter races | `AtomicInteger` or `synchronized` |
| **Inventory** | Concurrent stock deductions → negative stock | `synchronized` on `StockEntry` |
| **LRU Cache** | Concurrent get/put → inconsistent list state | `ReentrantReadWriteLock` |
| **Task Scheduler** | Main loop + task execution threads | `PriorityBlockingQueue` |
| **Pub/Sub** | Concurrent subscribe + publish | `CopyOnWriteArrayList` for subscribers |

---

## Thread-Safe LRU Cache Example

```java
import java.util.concurrent.locks.*;

public class ThreadSafeLRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);  // sentinel
    private final Node<K, V> tail = new Node<>(null, null);  // sentinel
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private static class Node<K, V> {
        K key; V value;
        Node<K, V> prev, next;
        Node(K key, V value) { this.key = key; this.value = value; }
    }

    public ThreadSafeLRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public Optional<V> get(K key) {
        rwLock.writeLock().lock();   // write lock because we move node to front
        try {
            Node<K, V> node = map.get(key);
            if (node == null) return Optional.empty();
            moveToFront(node);
            return Optional.of(node.value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            if (map.containsKey(key)) {
                Node<K, V> node = map.get(key);
                node.value = value;
                moveToFront(node);
            } else {
                if (map.size() == capacity) {
                    Node<K, V> lru = removeLast();
                    map.remove(lru.key);
                }
                Node<K, V> newNode = new Node<>(key, value);
                addToFront(newNode);
                map.put(key, newNode);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToFront(Node<K, V> node) { remove(node); addToFront(node); }

    private Node<K, V> removeLast() {
        Node<K, V> last = tail.prev;
        remove(last);
        return last;
    }
}
```
