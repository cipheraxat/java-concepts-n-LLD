package lessons.collections_framework;

import java.util.*;

/**
 * LESSON 02F — Advanced Map Implementations: EnumMap, WeakHashMap, and Performance Optimization
 *
 * This lesson covers specialized Map implementations not covered in MapDemo.java:
 *   - EnumMap: high-performance map for enum keys
 *   - WeakHashMap: entries automatically removed when keys are garbage-collected
 *   - LRU Cache (recap + manual implementation with LinkedHashMap)
 *
 * SDE2 Interview:
 *  - "When would you use EnumMap?"
 *    => When all keys are from a single enum type. Backed by arrays, extremely fast,
 *       compact memory, maintains enum declaration order.
 *  - "What is WeakHashMap and when is it useful?"
 *    => Keys are held via WeakReferences. Entries are auto-removed after key is GC'd.
 *       Useful for caches, metadata maps, and canonicalization maps where you don't want
 *       the map to prevent garbage collection of keys.
 *  - "Difference between WeakHashMap and HashMap?"
 *    => HashMap holds strong references to keys (prevents GC). WeakHashMap holds weak references,
 *       allowing GC to collect keys. Stale entries are lazily cleaned on map operations.
 *  - "How to implement an LRU Cache?"
 *    => LinkedHashMap with accessOrder=true + override removeEldestEntry,
 *       OR use a doubly-linked list + HashMap for O(1) get/put/evict.
 */
public class AdvancedMapDemo {

    // ═══════════════════════════════════════════════════════════════════════
    // 1. ENUMMAP — enum keys only, array-backed, fastest Map
    // ═══════════════════════════════════════════════════════════════════════

    enum HttpMethod { GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS }

    enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    static void enumMapDemo() {
        System.out.println("=== EnumMap ===");

        // Constructor requires the enum class
        EnumMap<HttpMethod, String> descriptions = new EnumMap<>(HttpMethod.class);
        descriptions.put(HttpMethod.GET, "Retrieve resource");
        descriptions.put(HttpMethod.POST, "Create resource");
        descriptions.put(HttpMethod.PUT, "Update/replace resource");
        descriptions.put(HttpMethod.DELETE, "Remove resource");
        descriptions.put(HttpMethod.PATCH, "Partial update");

        // Iteration follows ENUM DECLARATION ORDER (not insertion order)
        System.out.println("EnumMap (declaration order):");
        descriptions.forEach((method, desc) ->
            System.out.println("  " + method + " → " + desc));

        // Null values are allowed, null keys are NOT
        descriptions.put(HttpMethod.HEAD, null); // OK
        System.out.println("HEAD value (null): " + descriptions.get(HttpMethod.HEAD));

        // containsKey, containsValue, size — all O(1) or O(enum.size)
        System.out.println("Contains GET? " + descriptions.containsKey(HttpMethod.GET));
        System.out.println("Size: " + descriptions.size());

        // EnumMap as a counter/accumulator
        System.out.println("\n--- EnumMap as Counter ---");
        EnumMap<Priority, Integer> taskCounts = new EnumMap<>(Priority.class);
        // Initialize all counts to 0
        for (Priority p : Priority.values()) taskCounts.put(p, 0);

        // Simulate counting tasks
        Priority[] tasks = {Priority.HIGH, Priority.LOW, Priority.HIGH, Priority.MEDIUM,
                            Priority.CRITICAL, Priority.LOW, Priority.HIGH};
        for (Priority p : tasks) {
            taskCounts.merge(p, 1, Integer::sum);
        }
        System.out.println("Task counts: " + taskCounts);
        // {LOW=2, MEDIUM=1, HIGH=3, CRITICAL=1}

        // Copying from another Map
        Map<HttpMethod, String> regularMap = Map.of(HttpMethod.GET, "get", HttpMethod.POST, "post");
        EnumMap<HttpMethod, String> copied = new EnumMap<>(regularMap);
        System.out.println("Copied from Map: " + copied);

        // Performance: EnumMap is backed by Object[] indexed by enum ordinal
        // Much faster than HashMap<EnumType, V> — no hashing, no collision handling
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. WEAKHASHMAP — auto-evicting cache via weak references
    // ═══════════════════════════════════════════════════════════════════════

    static void weakHashMapDemo() throws InterruptedException {
        System.out.println("\n=== WeakHashMap ===");

        WeakHashMap<Object, String> cache = new WeakHashMap<>();

        // Create keys with strong references
        Object key1 = new Object();
        Object key2 = new Object();
        Object key3 = new Object();

        cache.put(key1, "value1");
        cache.put(key2, "value2");
        cache.put(key3, "value3");
        System.out.println("Before GC — size: " + cache.size()); // 3

        // Remove strong references to key2 and key3
        key2 = null;
        key3 = null;

        // Suggest garbage collection (not guaranteed, but usually works for demo)
        System.gc();
        Thread.sleep(100); // give GC time to run

        // Stale entries are lazily cleaned on next map operation
        System.out.println("After GC — size: " + cache.size()); // likely 1
        System.out.println("key1 still present: " + cache.containsValue("value1"));

        // Practical use: caching metadata about objects without preventing their GC
        System.out.println("\n--- WeakHashMap as Metadata Cache ---");
        WeakHashMap<String, Map<String, String>> metadataCache = new WeakHashMap<>();

        String resource = new String("connection-pool-1"); // avoid string interning for demo
        metadataCache.put(resource, Map.of("created", "2025-01-01", "pool-size", "10"));
        System.out.println("Metadata: " + metadataCache.get(resource));

        // When 'resource' goes out of scope and is GC'd, the metadata entry is auto-removed
        // No memory leak, unlike HashMap which would hold the entry forever

        // IMPORTANT: Do NOT use string literals as keys — they are interned and never GC'd
        // BAD:  cache.put("literal-key", "value");  // never evicted!
        // GOOD: cache.put(new String("key"), "value"); // can be evicted

        // WeakHashMap vs HashMap comparison
        System.out.println("\n--- WeakHashMap vs HashMap ---");
        System.out.println("HashMap:     Strong refs → prevents GC of keys");
        System.out.println("WeakHashMap: Weak refs   → allows GC of keys → auto-cleanup");
        System.out.println("Use WeakHashMap when:");
        System.out.println("  - Building caches that should not prevent object collection");
        System.out.println("  - Associating metadata with objects you don't own");
        System.out.println("  - Canonicalization maps (similar to String.intern())");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. LRU CACHE — LinkedHashMap approach + manual DoublyLinkedList+HashMap
    // ═══════════════════════════════════════════════════════════════════════

    // 3a. LinkedHashMap-based LRU Cache (simplest approach)
    static class LinkedHashMapLRU<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        LinkedHashMapLRU(int maxSize) {
            super(maxSize, 0.75f, true); // accessOrder = true
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    // 3b. Manual LRU Cache with O(1) get/put (classic interview implementation)
    static class LRUCache<K, V> {
        private final int capacity;
        private final Map<K, Node<K, V>> map;
        private final Node<K, V> head; // dummy head (MRU side)
        private final Node<K, V> tail; // dummy tail (LRU side)

        static class Node<K, V> {
            K key;
            V value;
            Node<K, V> prev, next;

            Node(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }

        LRUCache(int capacity) {
            this.capacity = capacity;
            this.map = new HashMap<>();
            this.head = new Node<>(null, null);
            this.tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
        }

        V get(K key) {
            Node<K, V> node = map.get(key);
            if (node == null) return null;
            moveToHead(node);
            return node.value;
        }

        void put(K key, V value) {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                moveToHead(existing);
            } else {
                Node<K, V> newNode = new Node<>(key, value);
                map.put(key, newNode);
                addToHead(newNode);
                if (map.size() > capacity) {
                    Node<K, V> lru = tail.prev;
                    removeNode(lru);
                    map.remove(lru.key);
                }
            }
        }

        private void addToHead(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        private void removeNode(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        private void moveToHead(Node<K, V> node) {
            removeNode(node);
            addToHead(node);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            Node<K, V> curr = head.next;
            while (curr != tail) {
                if (curr != head.next) sb.append(", ");
                sb.append(curr.key).append("=").append(curr.value);
                curr = curr.next;
            }
            return sb.append("}").toString();
        }
    }

    static void lruCacheDemo() {
        System.out.println("\n=== LRU Cache (LinkedHashMap) ===");
        LinkedHashMapLRU<Integer, String> lru1 = new LinkedHashMapLRU<>(3);
        lru1.put(1, "one");
        lru1.put(2, "two");
        lru1.put(3, "three");
        System.out.println("Initial: " + lru1);     // {1=one, 2=two, 3=three}

        lru1.get(1);                                  // access 1 → moves to end (MRU)
        lru1.put(4, "four");                          // evicts LRU (key=2)
        System.out.println("After get(1), put(4): " + lru1); // {3=three, 1=one, 4=four}

        System.out.println("\n=== LRU Cache (Manual — HashMap + DoublyLinkedList) ===");
        LRUCache<Integer, String> lru2 = new LRUCache<>(3);
        lru2.put(1, "one");
        lru2.put(2, "two");
        lru2.put(3, "three");
        System.out.println("Initial: " + lru2);

        lru2.get(1);
        lru2.put(4, "four");  // evicts key=2 (LRU)
        System.out.println("After get(1), put(4): " + lru2);
        System.out.println("get(2): " + lru2.get(2)); // null — was evicted
        System.out.println("get(1): " + lru2.get(1)); // one — still present
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. MAP PERFORMANCE COMPARISON
    // ═══════════════════════════════════════════════════════════════════════

    static void performanceComparison() {
        System.out.println("\n=== Map Implementation Comparison ===");
        System.out.println("╔══════════════════╦══════════╦═══════════════╦═══════════════════════════════╗");
        System.out.println("║ Implementation   ║ get/put  ║ Ordering      ║ Key Constraints               ║");
        System.out.println("╠══════════════════╬══════════╬═══════════════╬═══════════════════════════════╣");
        System.out.println("║ HashMap          ║ O(1)*    ║ None          ║ hashCode+equals required      ║");
        System.out.println("║ LinkedHashMap    ║ O(1)*    ║ Insertion/    ║ hashCode+equals required      ║");
        System.out.println("║                  ║          ║ Access order  ║                               ║");
        System.out.println("║ TreeMap          ║ O(log n) ║ Sorted (key)  ║ Comparable or Comparator      ║");
        System.out.println("║ EnumMap          ║ O(1)     ║ Enum decl.    ║ Enum keys only, no null keys  ║");
        System.out.println("║ WeakHashMap      ║ O(1)*    ║ None          ║ Keys are weak references      ║");
        System.out.println("║ Hashtable        ║ O(1)*    ║ None          ║ No null key/value (legacy)    ║");
        System.out.println("║ ConcurrentHashMap║ O(1)*    ║ None          ║ No null key/value             ║");
        System.out.println("╚══════════════════╩══════════╩═══════════════╩═══════════════════════════════╝");
        System.out.println("  * Amortized, assuming good hash distribution");
    }

    public static void main(String[] args) throws InterruptedException {
        enumMapDemo();
        weakHashMapDemo();
        lruCacheDemo();
        performanceComparison();
    }
}
