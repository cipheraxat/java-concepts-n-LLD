package lessons.collections_framework;

import java.util.*;

/**
 * LESSON 02A — List Implementations
 *
 * ArrayList  — dynamic array; O(1) random access; O(n) insert/remove at middle
 * LinkedList — doubly-linked; O(1) at head/tail; O(n) random access; implements Deque too
 * Vector     — like ArrayList but synchronized; legacy, prefer Collections.synchronizedList
 * Stack      — extends Vector; legacy, prefer ArrayDeque
 *
 * SDE2 Interview:
 *  - "When to use ArrayList vs LinkedList?"
 *    => ArrayList: frequent reads, infrequent inserts/removes in middle
 *       LinkedList: frequent inserts/removes at ends, usage as Queue/Deque
 *  - "How does ArrayList resize?" => Creates new array of 1.5x capacity, copies elements
 *  - "What is initial capacity of ArrayList?" => 10; can pre-size to avoid copies
 */
public class ListDemo {

    public static void main(String[] args) {

        // ── ArrayList ────────────────────────────────────────────────────────────
        System.out.println("=== ArrayList ===");
        List<String> arrayList = new ArrayList<>(16);  // pre-size to avoid resizing

        arrayList.add("Banana");
        arrayList.add("Apple");
        arrayList.add("Cherry");
        arrayList.add(1, "Avocado");      // insert at index — shifts elements right O(n)
        arrayList.addAll(List.of("Durian", "Elderberry"));

        System.out.println(arrayList);
        System.out.println("Index of Apple: " + arrayList.indexOf("Apple"));
        arrayList.remove("Banana");       // by value — O(n) search + shift
        arrayList.remove(0);              // by index — O(n) shift
        System.out.println("After removes: " + arrayList);

        // Iteration — prefer enhanced for or Iterator (do NOT index-loop a LinkedList)
        System.out.print("Enhanced for: ");
        for (String s : arrayList) System.out.print(s + " ");
        System.out.println();

        // subList view (backed by original — mutations reflect back)
        List<String> sub = arrayList.subList(0, 2);
        System.out.println("SubList(0,2): " + sub);

        // Convert array <-> List
        String[] arr = { "X", "Y", "Z" };
        List<String> fromArray = new ArrayList<>(Arrays.asList(arr));  // mutable list
        List<String> fixed     = Arrays.asList(arr);                   // fixed-size, backed by array
        List<String> immutable = List.of("A", "B", "C");               // Java 9+, truly immutable

        // ── ArrayList internal resize demo ───────────────────────────────────────
        System.out.println("\n=== ArrayList Resize Demo ===");
        ArrayList<Integer> nums = new ArrayList<>();  // initial capacity 10
        for (int i = 0; i < 20; i++) nums.add(i);    // triggers resize at 11th element
        System.out.println("Size: " + nums.size());   // 20

        // ── LinkedList ───────────────────────────────────────────────────────────
        System.out.println("\n=== LinkedList (as List + Deque) ===");
        LinkedList<String> ll = new LinkedList<>();
        ll.add("Middle");
        ll.addFirst("Head");    // O(1)
        ll.addLast("Tail");     // O(1)
        System.out.println(ll);

        ll.removeFirst();       // O(1)
        ll.removeLast();        // O(1)
        System.out.println("After removes: " + ll);

        // LinkedIn as Queue (FIFO)
        Queue<String> queue = new LinkedList<>();
        queue.offer("First");
        queue.offer("Second");
        queue.offer("Third");
        System.out.println("Queue peek: " + queue.peek());  // First
        System.out.println("Queue poll: " + queue.poll());  // First (removes)
        System.out.println("Queue: " + queue);

        // ── Immutable Lists ──────────────────────────────────────────────────────
        System.out.println("\n=== Immutable Lists ===");
        List<Integer> immutableNums = List.of(1, 2, 3, 4, 5);  // Java 9+
        System.out.println(immutableNums);
        try {
            immutableNums.add(6);  // UnsupportedOperationException
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot modify immutable list");
        }

        // ── Sort & Binary Search ─────────────────────────────────────────────────
        System.out.println("\n=== Sorting ===");
        List<Integer> sortable = new ArrayList<>(Arrays.asList(5, 3, 1, 4, 2));
        Collections.sort(sortable);                         // natural order
        System.out.println("Sorted: " + sortable);
        Collections.sort(sortable, Comparator.reverseOrder());
        System.out.println("Reverse: " + sortable);

        List<Integer> bsTarget = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        int idx = Collections.binarySearch(bsTarget, 3);   // MUST be sorted first!
        System.out.println("Binary search for 3: index " + idx);  // 2

        // ── List.copyOf, Collections.unmodifiableList ────────────────────────────
        System.out.println("\n=== Defensive copies ===");
        List<String> original = new ArrayList<>(List.of("a", "b", "c"));
        List<String> unmodifiable = Collections.unmodifiableList(original);  // view, still mutable via original
        List<String> copy = List.copyOf(original);  // Java 10+, truly independent + immutable
        original.add("d");
        System.out.println("Unmodifiable reflects change: " + unmodifiable); // [a, b, c, d]
        System.out.println("copyOf doesn't reflect change: " + copy);        // [a, b, c]
    }
}
