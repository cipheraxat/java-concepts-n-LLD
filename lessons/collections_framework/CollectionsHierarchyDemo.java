package lessons.collections_framework;

import java.util.*;

/**
 * LESSON 02E — Collections Framework: Architectural Hierarchy
 *
 * The Java Collections Framework is built around a set of core interfaces:
 *
 *                     Iterable<T>
 *                         │
 *                    Collection<T>
 *                    /     |      \
 *               List<T>  Set<T>  Queue<T>
 *                 │        │        │
 *            ┌────┴───┐  ┌─┴──┐   ┌┴────────┐
 *         ArrayList  LinkedList  HashSet  PriorityQueue
 *         Vector               TreeSet    ArrayDeque
 *         Stack(*)            LinkedHashSet
 *                              EnumSet
 *
 *   Map<K,V> (separate hierarchy, NOT part of Collection)
 *      ├── HashMap
 *      ├── LinkedHashMap
 *      ├── TreeMap
 *      ├── EnumMap
 *      ├── WeakHashMap
 *      └── Hashtable (legacy)
 *
 *   (*) Stack extends Vector (legacy — prefer ArrayDeque)
 *
 * SDE2 Interview:
 *  - "Explain the Collections hierarchy."
 *    => Iterable → Collection → List/Set/Queue. Map is separate. Each interface
 *       defines contracts; implementations differ in performance and ordering.
 *  - "Why is Map not part of Collection?"
 *    => Collection deals with single elements; Map deals with key-value pairs.
 *       add(E) vs put(K,V) — semantically different.
 *  - "Why are Vector and Stack considered legacy?"
 *    => They use synchronized methods (coarse-grained), causing unnecessary overhead.
 *       Prefer ArrayList + Collections.synchronizedList, or ArrayDeque for stacks.
 *  - "Difference between fail-fast and fail-safe iterators?"
 *    => fail-fast (ArrayList, HashMap): throw ConcurrentModificationException on structural modification.
 *       fail-safe (ConcurrentHashMap, CopyOnWriteArrayList): iterate over a snapshot/copy, no exception.
 */
public class CollectionsHierarchyDemo {

    public static void main(String[] args) {

        // ═══════════════════════════════════════════════════════════════════════
        // 1. THE LIST INTERFACE — ordered, allows duplicates, index-based access
        // ═══════════════════════════════════════════════════════════════════════

        // ── ArrayList: dynamic array, O(1) get, O(n) insert/remove at middle ──
        System.out.println("=== ArrayList ===");
        List<String> arrayList = new ArrayList<>();
        arrayList.add("A");
        arrayList.add("B");
        arrayList.add("C");
        arrayList.add("A"); // duplicates allowed
        System.out.println("ArrayList: " + arrayList);          // [A, B, C, A]
        System.out.println("get(1): " + arrayList.get(1));      // B — O(1) random access

        // ── LinkedList: doubly-linked, O(1) at head/tail, O(n) random access ──
        System.out.println("\n=== LinkedList ===");
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("X");
        linkedList.addFirst("Head");    // O(1)
        linkedList.addLast("Tail");     // O(1)
        System.out.println("LinkedList: " + linkedList);        // [Head, X, Tail]
        // Also implements Deque — can be used as queue or stack

        // ── Vector: legacy synchronized ArrayList ─────────────────────────────
        System.out.println("\n=== Vector (Legacy — avoid in new code) ===");
        Vector<String> vector = new Vector<>();
        vector.add("V1");
        vector.add("V2");
        vector.add("V3");
        System.out.println("Vector: " + vector);
        System.out.println("Capacity: " + vector.capacity()); // default 10
        // Vector doubles capacity on resize (ArrayList grows by 50%)
        // All methods are synchronized — unnecessary overhead for single-threaded use
        // Prefer: ArrayList + Collections.synchronizedList() when thread-safety needed

        // Modern alternative for thread-safe List:
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        syncList.add("safe1");
        syncList.add("safe2");
        // MUST manually synchronize iteration:
        synchronized (syncList) {
            for (String s : syncList) {
                System.out.println("  syncList item: " + s);
            }
        }

        // ── Stack: extends Vector, legacy LIFO ────────────────────────────────
        System.out.println("\n=== Stack (Legacy — use ArrayDeque instead) ===");
        Stack<String> stack = new Stack<>();
        stack.push("Bottom");
        stack.push("Middle");
        stack.push("Top");
        System.out.println("Stack: " + stack);
        System.out.println("peek: " + stack.peek());   // Top (does not remove)
        System.out.println("pop: " + stack.pop());      // Top (removes)
        System.out.println("search('Bottom'): " + stack.search("Bottom")); // 2 (1-based from top)
        System.out.println("Stack after pop: " + stack);

        // Stack has problems: it extends Vector (inherits index-based access, breaking LIFO contract)
        stack.add(0, "WRONG — inserted at bottom!"); // breaks stack semantics
        System.out.println("Stack abused: " + stack);

        // Preferred: ArrayDeque as stack
        System.out.println("\n=== ArrayDeque as Stack (preferred) ===");
        Deque<String> modernStack = new ArrayDeque<>();
        modernStack.push("Bottom");
        modernStack.push("Middle");
        modernStack.push("Top");
        System.out.println("ArrayDeque stack peek: " + modernStack.peek());
        System.out.println("ArrayDeque stack pop: " + modernStack.pop());

        // ═══════════════════════════════════════════════════════════════════════
        // 2. THE SET INTERFACE — no duplicates
        // ═══════════════════════════════════════════════════════════════════════

        // ── HashSet: O(1) add/remove/contains, unordered ─────────────────────
        System.out.println("\n=== HashSet (unordered, O(1)) ===");
        Set<String> hashSet = new HashSet<>();
        hashSet.add("Banana");
        hashSet.add("Apple");
        hashSet.add("Cherry");
        hashSet.add("Apple");  // duplicate — ignored
        System.out.println("HashSet: " + hashSet);          // unordered, no duplicates
        System.out.println("contains('Apple'): " + hashSet.contains("Apple")); // O(1)

        // ── LinkedHashSet: O(1) + insertion order ────────────────────────────
        System.out.println("\n=== LinkedHashSet (insertion order) ===");
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("Banana");
        linkedHashSet.add("Apple");
        linkedHashSet.add("Cherry");
        System.out.println("LinkedHashSet: " + linkedHashSet); // [Banana, Apple, Cherry]

        // ── TreeSet: O(log n), sorted (Red-Black Tree) ──────────────────────
        System.out.println("\n=== TreeSet (sorted, O(log n)) ===");
        TreeSet<String> treeSet = new TreeSet<>();
        treeSet.add("Banana");
        treeSet.add("Apple");
        treeSet.add("Cherry");
        treeSet.add("Date");
        System.out.println("TreeSet: " + treeSet);         // [Apple, Banana, Cherry, Date]
        System.out.println("first: " + treeSet.first());   // Apple
        System.out.println("last: " + treeSet.last());     // Date
        System.out.println("headSet('Cherry'): " + treeSet.headSet("Cherry")); // [Apple, Banana]
        System.out.println("tailSet('Cherry'): " + treeSet.tailSet("Cherry")); // [Cherry, Date]
        System.out.println("subSet('Banana','Date'): " + treeSet.subSet("Banana", "Date")); // [Banana, Cherry]
        // TreeSet does NOT allow null (NullPointerException)

        // ── EnumSet: most efficient Set for enum values ─────────────────────
        System.out.println("\n=== EnumSet ===");
        enum Color { RED, GREEN, BLUE, YELLOW, ORANGE, PURPLE }
        EnumSet<Color> warmColors = EnumSet.of(Color.RED, Color.YELLOW, Color.ORANGE);
        EnumSet<Color> coolColors = EnumSet.complementOf(warmColors);
        EnumSet<Color> allColors  = EnumSet.allOf(Color.class);
        System.out.println("Warm: " + warmColors);   // [RED, YELLOW, ORANGE]
        System.out.println("Cool: " + coolColors);   // [GREEN, BLUE, PURPLE]
        System.out.println("All: " + allColors);
        // Internally uses bit-vector — extremely fast, minimal memory

        // ═══════════════════════════════════════════════════════════════════════
        // 3. FAIL-FAST vs FAIL-SAFE ITERATORS
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("\n=== Fail-Fast Iterator (ConcurrentModificationException) ===");
        List<String> names = new ArrayList<>(List.of("Alice", "Bob", "Charlie"));
        try {
            for (String name : names) {
                if (name.equals("Bob")) {
                    names.remove(name); // structural modification during iteration → exception
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("ConcurrentModificationException caught!");
        }

        // Safe removal using Iterator.remove()
        System.out.println("\n=== Safe Removal with Iterator ===");
        List<String> names2 = new ArrayList<>(List.of("Alice", "Bob", "Charlie"));
        Iterator<String> it = names2.iterator();
        while (it.hasNext()) {
            if (it.next().equals("Bob")) {
                it.remove(); // safe — uses iterator's own remove
            }
        }
        System.out.println("After safe removal: " + names2); // [Alice, Charlie]

        // Or use removeIf (Java 8+)
        List<String> names3 = new ArrayList<>(List.of("Alice", "Bob", "Charlie"));
        names3.removeIf(n -> n.equals("Bob"));
        System.out.println("After removeIf: " + names3); // [Alice, Charlie]

        // ═══════════════════════════════════════════════════════════════════════
        // 4. COLLECTIONS UTILITY CLASS
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("\n=== Collections utility methods ===");
        List<Integer> nums = new ArrayList<>(List.of(5, 3, 1, 4, 2));

        Collections.sort(nums);
        System.out.println("Sorted: " + nums);

        Collections.reverse(nums);
        System.out.println("Reversed: " + nums);

        Collections.shuffle(nums);
        System.out.println("Shuffled: " + nums);

        System.out.println("Min: " + Collections.min(nums));
        System.out.println("Max: " + Collections.max(nums));
        System.out.println("Frequency of 3: " + Collections.frequency(nums, 3));

        // Unmodifiable wrappers
        List<Integer> unmod = Collections.unmodifiableList(nums);
        try {
            unmod.add(99);
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot modify unmodifiable list");
        }

        // Singleton collections
        Set<String> single = Collections.singleton("only");
        List<String> singleList = Collections.singletonList("only");
        System.out.println("Singleton: " + single);

        // Empty collections (immutable)
        List<Object> empty = Collections.emptyList();
        System.out.println("Empty list: " + empty);

        // ═══════════════════════════════════════════════════════════════════════
        // 5. SUMMARY: CHOOSING THE RIGHT COLLECTION
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("\n=== Collection Selection Guide ===");
        System.out.println("Need indexed access?       → ArrayList");
        System.out.println("Frequent insert/remove?    → LinkedList");
        System.out.println("No duplicates, unordered?  → HashSet");
        System.out.println("No duplicates, sorted?     → TreeSet");
        System.out.println("No duplicates, ordered?    → LinkedHashSet");
        System.out.println("FIFO queue?                → ArrayDeque");
        System.out.println("LIFO stack?                → ArrayDeque");
        System.out.println("Priority ordering?         → PriorityQueue");
        System.out.println("Thread-safe list?          → CopyOnWriteArrayList / synchronizedList");
        System.out.println("Thread-safe set?           → ConcurrentSkipListSet / CopyOnWriteArraySet");
        System.out.println("Enum values?               → EnumSet");
    }
}
