# Lesson 02 — Collections Framework (SDE2 Interview)

## Topics Covered
1. Collection hierarchy (Iterable → Collection → List/Set/Queue)
2. List: ArrayList vs LinkedList vs Vector vs CopyOnWriteArrayList
3. Set: HashSet vs LinkedHashSet vs TreeSet
4. Map: HashMap vs LinkedHashMap vs TreeMap vs ConcurrentHashMap vs Hashtable
5. Queue & Deque: PriorityQueue, ArrayDeque, LinkedList
6. Fail-fast vs Fail-safe iterators
7. Comparable vs Comparator
8. Collections utility class
9. Time complexity table (big O for core operations)

## Complexity Cheat Sheet

| Structure      | get    | add    | remove | contains | Notes                        |
|----------------|--------|--------|--------|----------|------------------------------|
| ArrayList      | O(1)   | O(1)*  | O(n)   | O(n)     | *amortized; shifts on remove |
| LinkedList     | O(n)   | O(1)   | O(1)   | O(n)     | Good for deque ops           |
| HashSet        | —      | O(1)*  | O(1)*  | O(1)*    | *avg; O(n) worst (collisions)|
| TreeSet        | —      | O(log n)| O(log n)| O(log n)| Sorted, uses red-black tree  |
| HashMap        | O(1)*  | O(1)*  | O(1)*  | O(1)*    | O(n) worst (all same bucket) |
| TreeMap        | O(log n)| O(log n)| O(log n)| O(log n)| NavigableMap, sorted by key  |
| PriorityQueue  | O(n)   | O(log n)| O(log n)| O(n)   | Min-heap by default          |
| ArrayDeque     | O(1)   | O(1)*  | O(1)   | O(n)     | Preferred over Stack/Queue   |

## Key Interview Points

| Question | Answer |
|---|---|
| HashMap vs Hashtable | HashMap: not synchronized, allows null key/value. Hashtable: synchronized, no nulls, legacy |
| HashMap vs ConcurrentHashMap | ConcurrentHashMap: segment-level locking (Java 8: CAS + synchronized per bucket), better concurrency |
| HashMap internal working | Array of Node[], hash(key) → index, chaining for collision; Java 8: tree-ify bucket at 8 nodes |
| Why HashMap capacity power of 2? | index = hash & (capacity-1) is faster than modulo |
| Fail-fast vs Fail-safe | Fail-fast (ArrayList, HashMap iterators): throws ConcurrentModificationException; Fail-safe (CopyOnWriteArrayList, ConcurrentHashMap): operates on copy |
| When to use LinkedHashMap? | When insertion order must be maintained in a Map |
| TreeMap vs HashMap | TreeMap: sorted, O(log n); HashMap: unsorted, O(1) avg |

## Files in this Lesson
- `ListDemo.java` — ArrayList, LinkedList, operations, iteration
- `SetDemo.java` — HashSet, LinkedHashSet, TreeSet
- `MapDemo.java` — HashMap internals, LinkedHashMap, TreeMap, ConcurrentHashMap
- `QueueDequeDemo.java` — PriorityQueue, ArrayDeque, Stack replacement
- `ComparatorComparableDemo.java` — Custom sorting
- `IteratorDemo.java` — Fail-fast, fail-safe, ListIterator
