# Lesson 05 — Multithreading & Concurrency (SDE2 Interview)

## Topics Covered
1. Thread creation: Thread, Runnable, Callable, Future
2. Thread lifecycle (NEW → RUNNABLE → BLOCKED/WAITING → TERMINATED)
3. `synchronized` — method-level and block-level
4. `volatile` keyword
5. `wait()`, `notify()`, `notifyAll()` — Object monitor
6. Executor framework: ExecutorService, ThreadPoolExecutor, ScheduledExecutorService
7. `java.util.concurrent` — CountDownLatch, CyclicBarrier, Semaphore, Phaser
8. Concurrent collections: ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue
9. Atomic classes: AtomicInteger, AtomicReference, AtomicLong
10. Locks: ReentrantLock, ReadWriteLock, StampedLock
11. CompletableFuture — async programming
12. Common problems: race condition, deadlock, livelock, starvation

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| `synchronized` vs `ReentrantLock` | ReentrantLock: tryLock (non-blocking), timed lock, interruptible, fairness mode |
| `volatile` vs `synchronized` | volatile: visibility only (no atomicity); synchronized: visibility + atomicity |
| When is volatile enough? | Single shared flag (stop flag), simple reads/writes where no compound ops |
| Thread pool core vs max size | Core: min threads kept alive. Max: max threads. Queue fills between core and max |
| How to detect deadlock? | Circular wait. Prevention: always acquire locks in same order |
| CountDownLatch vs CyclicBarrier | CountDownLatch: one-time, threads wait for count→0; CyclicBarrier: reusable, threads meet at barrier |
| ConcurrentHashMap vs synchronized HashMap | ConcurrentHashMap: bucket-level sync (CAS), no global lock, better throughput |

## Files in this Lesson
- `ThreadBasicsDemo.java` — Thread, Runnable, Callable, thread lifecycle
- `SynchronizationDemo.java` — synchronized, volatile, atomic operations
- `ExecutorServiceDemo.java` — thread pools, Callable, Future, CompletableFuture
- `ConcurrentUtilsDemo.java` — CountDownLatch, CyclicBarrier, Semaphore, BlockingQueue
- `LockDemo.java` — ReentrantLock, ReadWriteLock
- `DeadlockDemo.java` — deadlock example + prevention
