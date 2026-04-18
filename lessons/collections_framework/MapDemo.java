package lessons.collections_framework;

import java.util.*;

/**
 * LESSON 02B — HashMap Internals + Map Implementations
 *
 * SDE2 MUST know HashMap internals deeply — it's a top interview topic.
 *
 * HashMap Internal Working:
 *  1. Backed by array of Node<K,V>[] (default capacity 16)
 *  2. index = (n-1) & hash(key)  where hash() spreads bits to reduce collisions
 *  3. Collision handling: chaining (linked list per bucket)
 *  4. Java 8+: when bucket size >= TREEIFY_THRESHOLD (8) → convert to Red-Black Tree → O(log n)
 *  5. Resize (rehash) when load factor (0.75) exceeded: new capacity = 2x, re-bucket all keys
 *
 * SDE2 Interview Questions:
 *  - "What happens when two keys have same hashCode?" => Same bucket, chain via equals()
 *  - "Why load factor 0.75?" => Balance between space and collision probability
 *  - "Why capacity is always power of 2?" => index = hash & (capacity-1) instead of hash % capacity
 *  - "What is ConcurrentHashMap's locking strategy in Java 8?"
 *    => CAS for empty bucket, synchronized on first node for collision; no global lock
 */
public class  MapDemo {

    public static void main(String[] args) {

        // ── HashMap ──────────────────────────────────────────────────────────────
        System.out.println("=== HashMap ===");
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        scores.put("Charlie", 92);
        scores.put(null, 0);          // HashMap allows ONE null key

        System.out.println("Alice: " + scores.get("Alice"));
        System.out.println("Unknown: " + scores.get("Unknown"));  // null
        System.out.println("getOrDefault: " + scores.getOrDefault("Unknown", -1)); // -1

        // putIfAbsent, computeIfAbsent, merge — Java 8 Map additions
        scores.putIfAbsent("Alice", 100);    // Alice already exists — no change
        System.out.println("Alice (no change): " + scores.get("Alice"));

        scores.computeIfAbsent("Dave", k -> k.length() * 10); // Dave=40
        System.out.println("Dave computed: " + scores.get("Dave")); // 40

        // merge: combine old + new value
        scores.merge("Alice", 5, Integer::sum);  // Alice = 95 + 5 = 100
        System.out.println("Alice merged: " + scores.get("Alice"));

        // forEach
        System.out.println("All scores:");
        scores.forEach((k, v) -> System.out.println("  " + k + " -> " + v));

        // Iterating entries
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
             entry.setValue(entry.getValue() + 1);  // Can mutate during entrySet iteration
        }

        // other way to iterate keys/values
        for (String key : scores.keySet()) {
            System.out.println("Key: " + key);
        }
        for (Integer value : scores.values()) {
            System.out.println("Value: " + value);
        }

        System.out.println("Keys: " + scores.keySet());
        System.out.println("Values: " + scores.values());

        // ── LinkedHashMap — insertion order preserved ─────────────────────────────
        System.out.println("\n=== LinkedHashMap (insertion order) ===");
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("Charlie", 1);
        linked.put("Alice", 2);
        linked.put("Bob", 3);
        System.out.println(linked);  // {Charlie=1, Alice=2, Bob=3} — insertion order

        // LRU Cache using LinkedHashMap (access-order mode)
        Map<Integer, String> lruCache = new LinkedHashMap<>(16, 0.75f, true) {  // accessOrder=true
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return size() > 3;  // keep max 3 entries
            }
        };
        lruCache.put(1, "one"); lruCache.put(2, "two"); lruCache.put(3, "three");
        lruCache.get(1);        // 1 is now most recently used
        lruCache.put(4, "four"); // triggers eviction of LRU = 2
        System.out.println("LRU Cache: " + lruCache);  // {3=three, 1=one, 4=four}

        // ── TreeMap — sorted by key ──────────────────────────────────────────────
        System.out.println("\n=== TreeMap (sorted) ===");
        TreeMap<String, Integer> tree = new TreeMap<>();
        tree.put("Banana", 2); tree.put("Apple", 1); tree.put("Cherry", 3);
        System.out.println(tree);                          // {Apple=1, Banana=2, Cherry=3}
        System.out.println("First key: " + tree.firstKey());  // Apple
        System.out.println("Last key: " + tree.lastKey());    // Cherry
        System.out.println("Floor key of 'B': " + tree.floorKey("B")); // Banana
        System.out.println("Keys <= Banana: " + tree.headMap("Cherry"));  // headMap is exclusive end
        System.out.println("Desc keys: " + tree.descendingKeySet());

        // ── ConcurrentHashMap ────────────────────────────────────────────────────
        System.out.println("\n=== ConcurrentHashMap ===");
        Map<String, Integer> concurrentMap = new java.util.concurrent.ConcurrentHashMap<>();
        concurrentMap.put("key1", 1);
        concurrentMap.put("key2", 2);
        // ConcurrentHashMap: no null keys or values (unlike HashMap)
        // Thread-safe: uses CAS for empty buckets, synchronized on first node for collision
        // Does NOT throw ConcurrentModificationException during iteration (fail-safe)

        // Atomic operation
        concurrentMap.compute("key1", (k, v) -> v == null ? 1 : v + 1);
        System.out.println("After compute: " + concurrentMap);

        // ── Frequency count pattern (very common in interviews) ──────────────────
        System.out.println("\n=== Frequency Count Pattern ===");
        String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            freq.merge(w, 1, Integer::sum);   // elegant merge pattern
            // equivalent: freq.put(w, freq.getOrDefault(w, 0) + 1);
        }
        System.out.println("Frequencies: " + freq);

        // Sort by value (very common interview task)
        freq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
    }
}
