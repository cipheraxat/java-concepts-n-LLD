package lessons.multithreading_concurrency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * LESSON 05D — Concurrent Collections for Multi-Threaded Environments
 *
 * Covers thread-safe collections designed for high concurrency:
 *   - ConcurrentHashMap: lock-striping, CAS-based thread-safe map
 *   - ConcurrentSkipListMap: sorted concurrent map (concurrent TreeMap)
 *   - BlockingQueues: ArrayBlockingQueue, LinkedBlockingQueue, PriorityBlockingQueue
 *   - Producer-Consumer patterns with different BlockingQueue implementations
 *
 * SDE2 Interview:
 *  - "How does ConcurrentHashMap achieve thread safety in Java 8+?"
 *    => CAS (Compare-And-Swap) for empty buckets; synchronized on the first node of the
 *       bucket for collisions. No segment-level locking (that was Java 7).
 *       Reads are non-blocking; only writes lock at bucket level.
 *  - "Can ConcurrentHashMap have null keys/values?"
 *    => NO. Null is ambiguous — can't distinguish absent from null value in concurrent context.
 *  - "ConcurrentHashMap vs Collections.synchronizedMap?"
 *    => synchronizedMap: single lock for ALL operations → poor concurrency.
 *       ConcurrentHashMap: fine-grained locking → multiple threads can read/write simultaneously.
 *  - "What is ConcurrentSkipListMap?"
 *    => Thread-safe sorted map. O(log n) put/get/remove. Based on skip list, not Red-Black tree.
 *       Supports NavigableMap operations like floorKey, ceilingKey.
 *  - "Difference between ArrayBlockingQueue and LinkedBlockingQueue?"
 *    => ArrayBlockingQueue: bounded, array-backed, single lock for put+take.
 *       LinkedBlockingQueue: optionally bounded, linked-node, separate locks for put and take
 *       (better throughput under high contention).
 */
public class ConcurrentCollectionsDemo {

    // ═══════════════════════════════════════════════════════════════════════
    // 1. CONCURRENTHASHMAP — deep dive
    // ═══════════════════════════════════════════════════════════════════════

    static void concurrentHashMapDemo() throws InterruptedException {
        System.out.println("=== ConcurrentHashMap Deep Dive ===");

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // Basic operations — same API as HashMap
        map.put("Alice", 95);
        map.put("Bob", 87);
        map.put("Charlie", 72);
        System.out.println("Initial: " + map);

        // ── Atomic compound operations (critical for thread safety) ──────────
        System.out.println("\n--- Atomic Operations ---");

        // putIfAbsent: only puts if key doesn't exist (atomic)
        Integer prev = map.putIfAbsent("Alice", 100); // Alice exists → no change
        System.out.println("putIfAbsent Alice (existed): " + prev); // 95

        prev = map.putIfAbsent("Dave", 88);           // Dave doesn't exist → inserts
        System.out.println("putIfAbsent Dave (new): " + prev);     // null

        // computeIfAbsent: atomically compute value if absent
        map.computeIfAbsent("Eve", k -> k.length() * 10);
        System.out.println("computeIfAbsent Eve: " + map.get("Eve")); // 30

        // compute: atomically update (or create) value
        map.compute("Alice", (k, v) -> v == null ? 0 : v + 5);
        System.out.println("compute Alice: " + map.get("Alice")); // 100

        // merge: atomic read-modify-write
        map.merge("Bob", 10, Integer::sum); // Bob = 87 + 10 = 97
        System.out.println("merge Bob: " + map.get("Bob")); // 97

        // replace: atomic conditional replace
        boolean replaced = map.replace("Charlie", 72, 80); // only if value is 72
        System.out.println("replace Charlie 72→80: " + replaced);

        // ── Bulk operations (Java 8+) — parallel within the map ──────────────
        System.out.println("\n--- Bulk Operations (parallelismThreshold) ---");
        // parallelismThreshold: how many elements before going parallel
        // 1 = always parallel; Long.MAX_VALUE = always sequential

        // forEach with parallelism
        map.forEach(2, (k, v) ->
            System.out.println("  [" + Thread.currentThread().getName() + "] " + k + "=" + v));

        // search: find first match (returns null to keep searching)
        String richPerson = map.search(1, (k, v) -> v > 90 ? k : null);
        System.out.println("First with score > 90: " + richPerson);

        // reduce: aggregate all values
        int total = map.reduce(1, (k, v) -> v, Integer::sum);
        System.out.println("Total score: " + total);

        // ── Thread-safety demonstration ──────────────────────────────────────
        System.out.println("\n--- Concurrent Write Safety ---");
        ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
        int numThreads = 10;
        int incrementsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counters.computeIfAbsent("counter", k -> new AtomicInteger(0))
                            .incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("Expected: " + (numThreads * incrementsPerThread));
        System.out.println("Actual:   " + counters.get("counter").get());
        // Always matches — ConcurrentHashMap + AtomicInteger = fully thread-safe

        // ── ConcurrentHashMap does NOT allow null ────────────────────────────
        try {
            map.put(null, 1); // NullPointerException
        } catch (NullPointerException e) {
            System.out.println("\nNull key → NullPointerException");
        }
        try {
            map.put("test", null); // NullPointerException
        } catch (NullPointerException e) {
            System.out.println("Null value → NullPointerException");
        }

        // KeySetView — thread-safe Set backed by the map
        ConcurrentHashMap.KeySetView<String, Integer> keySet = map.keySet(0);
        keySet.add("NewKey"); // adds with default value 0
        System.out.println("After keySet.add: " + map);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. CONCURRENTSKIPLISTMAP — sorted concurrent map
    // ═══════════════════════════════════════════════════════════════════════

    static void concurrentSkipListMapDemo() throws InterruptedException {
        System.out.println("\n=== ConcurrentSkipListMap (Sorted + Thread-Safe) ===");

        ConcurrentSkipListMap<String, Integer> sortedMap = new ConcurrentSkipListMap<>();
        sortedMap.put("Charlie", 72);
        sortedMap.put("Alice", 95);
        sortedMap.put("Bob", 87);
        sortedMap.put("Dave", 105);
        sortedMap.put("Eve", 65);

        // Always sorted by natural key order
        System.out.println("Sorted map: " + sortedMap);
        // {Alice=95, Bob=87, Charlie=72, Dave=105, Eve=65}

        // ── NavigableMap operations ──────────────────────────────────────────
        System.out.println("\n--- NavigableMap Operations ---");
        System.out.println("firstKey: " + sortedMap.firstKey());         // Alice
        System.out.println("lastKey: " + sortedMap.lastKey());           // Eve
        System.out.println("floorKey('Cat'): " + sortedMap.floorKey("Cat"));     // Charlie (<=)
        System.out.println("ceilingKey('Cat'): " + sortedMap.ceilingKey("Cat")); // Charlie (>=)
        System.out.println("lowerKey('Charlie'): " + sortedMap.lowerKey("Charlie")); // Bob (<)
        System.out.println("higherKey('Charlie'): " + sortedMap.higherKey("Charlie")); // Dave (>)

        // Sub-map views (thread-safe)
        System.out.println("headMap('Charlie'): " + sortedMap.headMap("Charlie")); // {Alice, Bob}
        System.out.println("tailMap('Charlie'): " + sortedMap.tailMap("Charlie")); // {Charlie, Dave, Eve}
        System.out.println("subMap('Bob','Dave'): " + sortedMap.subMap("Bob", "Dave")); // {Bob, Charlie}

        System.out.println("descendingMap: " + sortedMap.descendingMap());

        // ── Concurrent access demonstration ─────────────────────────────────
        System.out.println("\n--- Concurrent Writes to Sorted Map ---");
        ConcurrentSkipListMap<Integer, String> concMap = new ConcurrentSkipListMap<>();
        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            int threadId = t;
            new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    int key = threadId * 10 + i;
                    concMap.put(key, "T" + threadId + "-" + i);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("ConcurrentSkipListMap size: " + concMap.size()); // 25
        System.out.println("Always sorted: " + concMap.firstKey() + " ... " + concMap.lastKey());

        // ConcurrentSkipListMap vs TreeMap
        System.out.println("\n--- ConcurrentSkipListMap vs TreeMap ---");
        System.out.println("TreeMap:               O(log n), NOT thread-safe, Red-Black tree");
        System.out.println("ConcurrentSkipListMap: O(log n), thread-safe, skip list");
        System.out.println("Use ConcurrentSkipListMap when you need sorted + concurrent access");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. BLOCKING QUEUES — for producer-consumer patterns
    // ═══════════════════════════════════════════════════════════════════════

    // ── 3a. ArrayBlockingQueue — bounded, single-lock ────────────────────
    static void arrayBlockingQueueDemo() throws InterruptedException {
        System.out.println("\n=== ArrayBlockingQueue (Bounded) ===");
        // Fixed capacity — blocks producer when full, consumer when empty
        ArrayBlockingQueue<String> abq = new ArrayBlockingQueue<>(3);

        // offer vs put
        abq.offer("item1");                           // non-blocking, returns false if full
        abq.offer("item2", 1, TimeUnit.SECONDS);     // wait up to 1s
        abq.put("item3");                             // blocks until space available
        System.out.println("Queue full: " + abq);

        // peek vs poll vs take
        System.out.println("peek: " + abq.peek());                           // item1 (no remove)
        System.out.println("poll: " + abq.poll());                           // item1 (removes)
        System.out.println("poll with timeout: " + abq.poll(1, TimeUnit.SECONDS)); // item2

        // drainTo — bulk removal
        abq.offer("a"); abq.offer("b"); abq.offer("c");
        List<String> drained = new ArrayList<>();
        abq.drainTo(drained, 2); // drain up to 2 elements
        System.out.println("Drained: " + drained);
        System.out.println("Remaining: " + abq);

        // Fair mode (FIFO ordering of waiting threads)
        ArrayBlockingQueue<String> fairQueue = new ArrayBlockingQueue<>(10, true); // fair=true
        // Ensures longest-waiting thread gets served first (slight performance cost)
    }

    // ── 3b. LinkedBlockingQueue — optionally bounded, separate locks ─────
    static void linkedBlockingQueueDemo() throws InterruptedException {
        System.out.println("\n=== LinkedBlockingQueue ===");
        // Separate locks for head (take) and tail (put) → better throughput than ABQ

        // Bounded
        LinkedBlockingQueue<Integer> bounded = new LinkedBlockingQueue<>(100);
        // Unbounded (capacity = Integer.MAX_VALUE — careful: can cause OOM)
        LinkedBlockingQueue<Integer> unbounded = new LinkedBlockingQueue<>();

        for (int i = 0; i < 5; i++) bounded.put(i);
        System.out.println("LinkedBlockingQueue: " + bounded);
        System.out.println("Size: " + bounded.size());
        System.out.println("Remaining capacity: " + bounded.remainingCapacity()); // 95

        System.out.println("\n--- ArrayBlockingQueue vs LinkedBlockingQueue ---");
        System.out.println("ArrayBlockingQueue:  bounded, single lock, predictable memory");
        System.out.println("LinkedBlockingQueue: optionally bounded, dual locks, better throughput");
        System.out.println("Use ABQ when: capacity is fixed and known");
        System.out.println("Use LBQ when: need higher throughput under contention");
    }

    // ── 3c. PriorityBlockingQueue — unbounded priority queue ─────────────
    static void priorityBlockingQueueDemo() throws InterruptedException {
        System.out.println("\n=== PriorityBlockingQueue ===");
        // Unbounded, thread-safe PriorityQueue
        // take() blocks when empty; put() never blocks (unbounded)

        PriorityBlockingQueue<Integer> pbq = new PriorityBlockingQueue<>();
        pbq.put(5); pbq.put(1); pbq.put(3); pbq.put(2); pbq.put(4);

        System.out.print("Priority order (min-heap): ");
        while (!pbq.isEmpty()) {
            System.out.print(pbq.take() + " "); // 1 2 3 4 5
        }
        System.out.println();

        // With custom comparator (max-heap)
        record Task(String name, int priority) implements Comparable<Task> {
            @Override
            public int compareTo(Task other) {
                return Integer.compare(other.priority, this.priority); // higher = first
            }
        }

        PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
        taskQueue.put(new Task("Low", 1));
        taskQueue.put(new Task("Critical", 10));
        taskQueue.put(new Task("Medium", 5));

        System.out.println("Tasks by priority:");
        while (!taskQueue.isEmpty()) {
            System.out.println("  " + taskQueue.take());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. PRODUCER-CONSUMER with different BlockingQueues
    // ═══════════════════════════════════════════════════════════════════════

    static void producerConsumerDemo() throws InterruptedException {
        System.out.println("\n=== Producer-Consumer: ArrayBlockingQueue ===");
        producerConsumer(new ArrayBlockingQueue<>(5), "ABQ");

        System.out.println("\n=== Producer-Consumer: LinkedBlockingQueue ===");
        producerConsumer(new LinkedBlockingQueue<>(5), "LBQ");
    }

    private static void producerConsumer(BlockingQueue<Integer> queue, String label)
            throws InterruptedException {
        int itemCount = 8;
        int poisonPill = -1; // sentinel to signal consumer to stop

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    queue.put(i); // blocks if queue is full
                    System.out.printf("[%s] Produced: %d (queue size: %d)%n",
                        label, i, queue.size());
                    Thread.sleep(30);
                }
                queue.put(poisonPill); // signal end
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    int item = queue.take(); // blocks if queue is empty
                    if (item == poisonPill) break;
                    System.out.printf("[%s] Consumed: %d (queue size: %d)%n",
                        label, item, queue.size());
                    Thread.sleep(70); // consumer is slower → queue fills up
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. SUMMARY TABLE: Concurrent Collections
    // ═══════════════════════════════════════════════════════════════════════

    static void printSummary() {
        System.out.println("\n=== Concurrent Collections Summary ===");
        System.out.println("╔════════════════════════════╦════════════════════╦════════════════════════════════╗");
        System.out.println("║ Collection                 ║ Type               ║ Key Characteristics            ║");
        System.out.println("╠════════════════════════════╬════════════════════╬════════════════════════════════╣");
        System.out.println("║ ConcurrentHashMap          ║ Map                ║ CAS + node-sync, no null k/v   ║");
        System.out.println("║ ConcurrentSkipListMap      ║ Sorted Map         ║ O(log n), NavigableMap         ║");
        System.out.println("║ ConcurrentSkipListSet      ║ Sorted Set         ║ Backed by ConcurrentSkipListMap║");
        System.out.println("║ CopyOnWriteArrayList       ║ List               ║ Copies on write, fast reads    ║");
        System.out.println("║ CopyOnWriteArraySet        ║ Set                ║ Backed by CopyOnWriteArrayList ║");
        System.out.println("║ ArrayBlockingQueue         ║ Bounded Queue      ║ Single lock, fixed capacity    ║");
        System.out.println("║ LinkedBlockingQueue        ║ Optionally Bounded ║ Dual locks, higher throughput  ║");
        System.out.println("║ PriorityBlockingQueue      ║ Unbounded PQ       ║ Thread-safe heap               ║");
        System.out.println("║ SynchronousQueue           ║ Zero-capacity      ║ Direct handoff (no buffering)  ║");
        System.out.println("║ DelayQueue                 ║ Delayed elements   ║ Elements available after delay ║");
        System.out.println("╚════════════════════════════╩════════════════════╩════════════════════════════════╝");
    }

    public static void main(String[] args) throws Exception {
        concurrentHashMapDemo();
        concurrentSkipListMapDemo();
        arrayBlockingQueueDemo();
        linkedBlockingQueueDemo();
        priorityBlockingQueueDemo();
        producerConsumerDemo();
        printSummary();
    }
}
