# LRU Cache

**Difficulty:** Medium | **Companies:** Amazon, Google, Facebook, Microsoft

---

## Requirements

> "Design a Least Recently Used (LRU) cache with O(1) get and put operations, a fixed capacity, and automatic eviction of the least recently used entry when full."

### Clarifying Questions

> **You:** "What are the key and value types?"
>
> **Interviewer:** "Generic — support any key-value types."

> **You:** "What happens on a put when the cache is full?"
>
> **Interviewer:** "Evict the least recently used entry and insert the new one."

> **You:** "Does a get operation count as a 'use'?"
>
> **Interviewer:** "Yes. Both get and put move the entry to most-recently-used."

> **You:** "Thread safety?"
>
> **Interviewer:** "Support concurrent access with a read-write lock."

### Final Requirements

```
Requirements:
1. get(key) → return value if present, mark as most recently used. O(1).
2. put(key, value) → insert or update. Mark as most recently used. O(1).
3. When capacity is exceeded on put, evict the least recently used entry.
4. Thread-safe for concurrent access.

Out of scope:
- TTL-based expiry
- Distributed caching
- Cache statistics / hit-rate tracking
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **LRUCache** | Public API: get and put. Orchestrates the HashMap and doubly-linked list. Owns the read-write lock. |
| **DoublyLinkedList** | Maintains access order. Head = most recently used, tail = least recently used. O(1) add/remove via sentinel nodes. |
| **Node** | Doubly-linked list node holding key, value, prev, and next pointers. |

---

## Class Design

### LRUCache

| Requirement | What LRUCache must track |
|-------------|--------------------------|
| "O(1) get" | HashMap: key → Node for direct lookup |
| "O(1) eviction of LRU" | Doubly-linked list for ordering; tail sentinel for O(1) eviction |
| "Thread safety" | ReentrantReadWriteLock |

```
class LRUCache<K, V>:
    - capacity: int
    - map: HashMap<K, Node<K,V>>
    - list: DoublyLinkedList<K,V>
    - lock: ReentrantReadWriteLock

    + get(key) → V?
    + put(key, value) → void
    - evict() → void

class DoublyLinkedList<K, V>:
    - head: Node  // sentinel
    - tail: Node  // sentinel

    + addFirst(node) → void
    + remove(node) → void
    + removeLast() → Node

class Node<K, V>:
    - key: K
    - value: V
    - prev: Node
    - next: Node
```

---

## Implementation

### Data Structure Insight

The combination of a HashMap and a doubly-linked list gives us:
- **HashMap**: O(1) lookup by key → directly to the Node
- **Doubly-linked list**: O(1) move-to-front (remove + addFirst) and O(1) evict-tail

Sentinel head and tail nodes eliminate null checks at boundaries.

```
Sentinel HEAD ↔ [MRU node] ↔ ... ↔ [LRU node] ↔ Sentinel TAIL
```

### LRUCache.get

```
get(key)
    readLock.lock()
    try:
        node = map[key]
        if node is null → return null
    finally:
        readLock.unlock()

    writeLock.lock()
    try:
        // Re-check under write lock (node might have been evicted)
        node = map[key]
        if node is null → return null
        list.remove(node)
        list.addFirst(node)
        return node.value
    finally:
        writeLock.unlock()
```

### LRUCache.put

```
put(key, value)
    writeLock.lock()
    try:
        if map contains key:
            node = map[key]
            node.value = value
            list.remove(node)
            list.addFirst(node)
        else:
            if map.size >= capacity:
                evict()
            node = new Node(key, value)
            map[key] = node
            list.addFirst(node)
    finally:
        writeLock.unlock()
```

### LRUCache.evict

```
evict()
    lru = list.removeLast()
    map.remove(lru.key)
```

### Complete Code Implementation

```java
public class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    public Node(K key, V value) {
        this.key = key;
        this.value = value;
    }

    // Sentinel constructor
    public Node() {
        this(null, null);
    }
}
```

```java
public class DoublyLinkedList<K, V> {
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public DoublyLinkedList() {
        head = new Node<>();
        tail = new Node<>();
        head.next = tail;
        tail.prev = head;
    }

    public void addFirst(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    public void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    public Node<K, V> removeLast() {
        if (tail.prev == head) return null; // empty
        Node<K, V> lru = tail.prev;
        remove(lru);
        return lru;
    }

    public boolean isEmpty() {
        return head.next == tail;
    }
}
```

```java
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final DoublyLinkedList<K, V> list;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
        this.map = new HashMap<>(capacity);
        this.list = new DoublyLinkedList<>();
    }

    public V get(K key) {
        rwLock.writeLock().lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) return null;
            list.remove(node);
            list.addFirst(node);
            return node.value;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                list.remove(existing);
                list.addFirst(existing);
            } else {
                if (map.size() >= capacity) {
                    evict();
                }
                Node<K, V> node = new Node<>(key, value);
                map.put(key, node);
                list.addFirst(node);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void evict() {
        Node<K, V> lru = list.removeLast();
        if (lru != null) {
            map.remove(lru.key);
        }
    }

    public int size() {
        rwLock.readLock().lock();
        try {
            return map.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
```

### Verification

```
Setup: LRUCache capacity=3

Step 1: put(A, 1)
  map={A→nodeA}, list: HEAD ↔ A ↔ TAIL

Step 2: put(B, 2)
  map={A, B}, list: HEAD ↔ B ↔ A ↔ TAIL

Step 3: put(C, 3)
  map={A, B, C}, list: HEAD ↔ C ↔ B ↔ A ↔ TAIL

Step 4: get(A)  → returns 1
  Remove A, addFirst → list: HEAD ↔ A ↔ C ↔ B ↔ TAIL

Step 5: put(D, 4) → cache full, evict LRU = B
  removeLast → B, map.remove(B)
  Insert D → list: HEAD ↔ D ↔ A ↔ C ↔ TAIL
  map={A, C, D}

Step 6: get(B) → returns null (evicted ✓)

Step 7: put(A, 10) → update existing
  A.value = 10, remove A, addFirst
  list: HEAD ↔ A ↔ D ↔ C ↔ TAIL

Step 8: put(E, 5) → evict LRU = C
  list: HEAD ↔ E ↔ A ↔ D ↔ TAIL
  map={A, D, E}
```

---

## Extensibility

### 1. "How would you add TTL-based expiry?"

> "Each Node gets an `expiresAt` timestamp set during put. On get, I check if the node is expired — if so, remove it and return null. For proactive cleanup, a background thread periodically scans and evicts expired entries. TTL and LRU eviction coexist: expired entries are removed on access, and capacity-based eviction still uses LRU order."

### 2. "How would you make this a distributed cache?"

> "I'd use consistent hashing to partition keys across cache nodes. Each node runs its own LRU cache locally. A client-side library hashes the key to determine which node to query. For replication, I'd use a write-through or write-behind strategy to a backing store."

### 3. "How would you add cache statistics?"

> "I'd add `AtomicLong` counters for hits, misses, and evictions, incremented in get/put/evict. A `getStats()` method returns a snapshot. These counters are lock-free (atomic) so they don't add contention."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should be able to explain why a HashMap alone isn't enough (no ordering) and why a linked list alone isn't enough (no O(1) lookup). Implementing a working LRU cache with a HashMap and doubly-linked list is the main goal. Sentinel nodes are a nice touch. Eviction should work correctly.

### Mid-level

Mid-level candidates should implement clean sentinel-based doubly-linked list operations without bugs in pointer manipulation. The get operation should correctly promote the accessed node. Thread safety with a ReentrantReadWriteLock is expected. You should discuss why the simpler read-lock-then-write-lock approach has a race condition and why get needs a write lock (it mutates list order).

### Senior

Senior candidates would discuss the trade-off of using a single write lock for get (simpler, correct) vs. a lock-free approach using ConcurrentHashMap with a striped linked list. You'd explain cache warming strategies, consistent hashing for distributed scenarios, and how to layer TTL on top of LRU. The implementation should be clean and bug-free on the first pass.