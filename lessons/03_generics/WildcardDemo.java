package lessons.generics;

import java.util.*;

/**
 * LESSON 03B — Wildcards and PECS Principle
 *
 * Three types of wildcards:
 *  ? (unbounded)        — unknown type, read-only as Object
 *  ? extends T (upper)  — some subtype of T — you can READ T values (Producer)
 *  ? super T   (lower)  — some supertype of T — you can WRITE T values (Consumer)
 *
 * PECS: Producer Extends, Consumer Super
 *  Mnemonic: "If you are PRODUCING (reading) values, use extends.
 *              If you are CONSUMING (writing) values, use super."
 *
 * SDE2 Interview Questions:
 *  - "What is PECS?"
 *    => Producer Extends, Consumer Super. Collections.copy(dest, src):
 *       src is Producer → List<? extends T>; dest is Consumer → List<? super T>
 *  - "Why is List<Integer> NOT a subtype of List<Number>?"
 *    => Generics are invariant. If it were, you could add a Double to what is really a List<Integer>.
 *       Arrays ARE covariant (Integer[] is subtype of Number[]) but that's an old Java design flaw.
 *  - "What can you do with List<?>?" => Only read (as Object) and call size()/clear(); cannot add
 */
public class WildcardDemo {

    // ─── 1. Upper Bounded Wildcard (? extends T) — Producer ─────────────────────
    // Can READ items as Number; CANNOT add (except null)
    // Use when the collection is providing values to your method
    static double sumOfList(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) sum += n.doubleValue();  // safe read
        // list.add(1.5);  // COMPILE ERROR — can't add to ? extends
        return sum;
    }

    // Works with List<Integer>, List<Double>, List<Float>, etc.
    static <T> void printAll(List<? extends T> list) {
        for (T item : list) System.out.print(item + " ");
        System.out.println();
    }

    // ─── 2. Lower Bounded Wildcard (? super T) — Consumer ───────────────────────
    // Can WRITE Integer (and subtypes) into the list; reads only as Object
    // Use when the collection is receiving values from your method
    static void addNumbers(List<? super Integer> list) {
        list.add(1);
        list.add(2);
        list.add(3);
        // Integer x = list.get(0);  // COMPILE ERROR — type is unknown supertype of Integer
        Object x = list.get(0);      // read is only safe as Object
    }

    // ─── 3. PECS in action — like Collections.copy ──────────────────────────────
    // src is Producer (we read from it)  → extends
    // dest is Consumer (we write to it)  → super
    static <T> void copy(List<? super T> dest, List<? extends T> src) {
        for (T item : src) dest.add(item);
    }

    // ─── 4. Unbounded wildcard (?) ───────────────────────────────────────────────
    // Use when type doesn't matter for the operation
    static void printListSize(List<?> list) {
        System.out.println("Size: " + list.size()); // OK — size() is on raw List
        // list.add("anything");  // COMPILE ERROR
        Object first = list.isEmpty() ? null : list.get(0);  // read only as Object
        System.out.println("First element: " + first);
    }

    // ─── 5. Generics are INVARIANT (vs Arrays which are COVARIANT) ───────────────
    static void invarianceDemo() {
        // Arrays are covariant (legacy design, can cause ArrayStoreException at runtime)
        Number[] numbers = new Integer[3];       // OK — covariant
        numbers[0] = 1;
        try {
            numbers[1] = 1.5;  // ArrayStoreException at runtime! Array is really Integer[]
        } catch (ArrayStoreException e) {
            System.out.println("ArrayStoreException: " + e.getMessage());
        }

        // Generics are invariant — safer
        // List<Number> list = new ArrayList<Integer>();  // COMPILE ERROR — invariant
        List<? extends Number> safeList = new ArrayList<Integer>();  // OK with wildcard
    }

    public static void main(String[] args) {
        System.out.println("=== Upper Bounded (? extends) ===");
        List<Integer> ints = Arrays.asList(1, 2, 3, 4, 5);
        List<Double>  dubs = Arrays.asList(1.1, 2.2, 3.3);
        System.out.println("Sum of int list: "    + sumOfList(ints));
        System.out.println("Sum of double list: " + sumOfList(dubs));

        printAll(ints);
        printAll(Arrays.asList("hello", "world"));

        System.out.println("\n=== Lower Bounded (? super) ===");
        List<Number> numbers = new ArrayList<>();
        addNumbers(numbers);   // List<Number> is-a List<? super Integer>
        System.out.println("Numbers list: " + numbers);

        List<Object> objects = new ArrayList<>();
        addNumbers(objects);   // List<Object> is-a List<? super Integer>
        System.out.println("Objects list: " + objects);

        System.out.println("\n=== PECS: copy ===");
        List<Integer> source = new ArrayList<>(Arrays.asList(10, 20, 30));
        List<Number> destination = new ArrayList<>();
        copy(destination, source);  // src=? extends Integer, dest=? super Integer
        System.out.println("Destination: " + destination);

        System.out.println("\n=== Unbounded Wildcard ===");
        printListSize(List.of(1, 2, 3));
        printListSize(List.of("a", "b"));

        System.out.println("\n=== Covariance vs Invariance ===");
        invarianceDemo();

        System.out.println("\n=== Wildcard Capture Summary ===");
        /*
         *  List<?>              — unknown type, read-only (Object)
         *  List<? extends Foo>  — some subtype of Foo, read as Foo (Producer)
         *  List<? super Foo>    — some supertype of Foo, write Foo (Consumer)
         *  List<Foo>            — exactly Foo, both read and write
         *
         *  Common use case for PECS:
         *  Collections.sort(List<T> list, Comparator<? super T> c)
         *    — Comparator is a Consumer of T (it compares T values)
         *    — ? super T means it can compare parent types too
         */
        List<String> sortMe = new ArrayList<>(Arrays.asList("c", "a", "b"));
        Comparator<Object> comp = Comparator.comparing(Object::toString);
        Collections.sort(sortMe, comp);  // Comparator<Object> works for List<String>
        System.out.println("Sorted with Comparator<Object>: " + sortMe);
    }
}
