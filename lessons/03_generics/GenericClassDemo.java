package lessons.generics;

import java.util.*;
import java.util.function.Function;

/**
 * LESSON 03A — Generic Classes and Methods
 *
 * Generics provide compile-time type safety and eliminate casts.
 * Without generics you'd use Object everywhere → ClassCastException at runtime.
 *
 * SDE2 Interview:
 *  - "What problem do generics solve?" => Type safety at compile time, eliminate runtime ClassCastException
 *  - "Can you have generic constructors?" => YES
 *  - "Multiple bounded types?" => <T extends Comparable<T> & Serializable>
 */
public class GenericClassDemo {

    // ─── 1. Generic Class ────────────────────────────────────────────────────────
    static class Pair<A, B> {
        private final A first;
        private final B second;

        Pair(A first, B second) { this.first = first; this.second = second; }

        A getFirst()  { return first; }
        B getSecond() { return second; }

        // Generic method inside generic class (can introduce new type param)
        <C> Pair<C, B> mapFirst(Function<A, C> mapper) {
            return new Pair<>(mapper.apply(first), second);
        }

        @Override public String toString() { return "(" + first + ", " + second + ")"; }
    }

    // ─── 2. Generic Class with Bounded Type ──────────────────────────────────────
    // T must implement Comparable<T> — enables comparison operations
    static class SortedList<T extends Comparable<T>> {
        private final List<T> items = new ArrayList<>();

        void add(T item) {
            items.add(item);
            Collections.sort(items);  // natural order via compareTo
        }

        T min() { return items.isEmpty() ? null : items.get(0); }
        T max() { return items.isEmpty() ? null : items.get(items.size() - 1); }
        List<T> getAll() { return Collections.unmodifiableList(items); }

        @Override public String toString() { return items.toString(); }
    }

    // ─── 3. Multiple Bounds ──────────────────────────────────────────────────────
    // T must extend both Comparable<T> AND implement Cloneable
    // Class bound must come FIRST if present: <T extends MyClass & Interface1 & Interface2>
    static <T extends Comparable<T> & Cloneable> T cloneAndMax(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    // ─── 4. Standalone Generic Methods ───────────────────────────────────────────
    // Type parameter declared before return type
    static <T> List<T> repeat(T value, int times) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < times; i++) result.add(value);
        return result;
    }

    static <T extends Number> double sum(List<T> list) {
        double total = 0;
        for (T item : list) total += item.doubleValue(); // doubleValue() from Number
        return total;
    }

    // Generic method — type inference from arguments
    static <K, V> Map<V, K> invertMap(Map<K, V> original) {
        Map<V, K> inverted = new HashMap<>();
        original.forEach((k, v) -> inverted.put(v, k));
        return inverted;
    }

    // ─── 5. Generic Stack implementation ─────────────────────────────────────────
    static class GenericStack<T> {
        private final Object[] elements;  // Cannot do new T[] due to erasure
        private int size = 0;

        @SuppressWarnings("unchecked")
        GenericStack(int capacity) { elements = new Object[capacity]; }

        void push(T item) {
            if (size == elements.length) throw new IllegalStateException("Stack full");
            elements[size++] = item;
        }

        @SuppressWarnings("unchecked")
        T pop() {
            if (size == 0) throw new EmptyStackException();
            T item = (T) elements[--size];
            elements[size] = null;  // prevent memory leak
            return item;
        }

        @SuppressWarnings("unchecked")
        T peek() { return size == 0 ? null : (T) elements[size - 1]; }

        boolean isEmpty() { return size == 0; }
        int size() { return size; }
    }

    // ─── 6. Recursive generic bound (self-referential) ───────────────────────────
    // Common pattern in builder classes and fluent APIs
    static abstract class Builder<T extends Builder<T>> {
        private String name;

        @SuppressWarnings("unchecked")
        T withName(String name) { this.name = name; return (T) this; }

        String getName() { return name; }
    }

    static class PersonBuilder extends Builder<PersonBuilder> {
        private int age;
        PersonBuilder withAge(int age) { this.age = age; return this; }
        String build() { return "Person{name=" + getName() + ", age=" + age + "}"; }
    }

    public static void main(String[] args) {
        System.out.println("=== Generic Pair ===");
        Pair<String, Integer> p = new Pair<>("Alice", 30);
        System.out.println(p);
        Pair<Integer, Integer> mapped = p.mapFirst(String::length);  // ("Alice", 30) → (5, 30)
        System.out.println("Mapped: " + mapped);

        System.out.println("\n=== Bounded Generic Class ===");
        SortedList<Integer> sl = new SortedList<>();
        sl.add(5); sl.add(1); sl.add(8); sl.add(3);
        System.out.println("Sorted: " + sl);
        System.out.println("Min: " + sl.min() + ", Max: " + sl.max());

        SortedList<String> words = new SortedList<>();
        words.add("Banana"); words.add("Apple"); words.add("Cherry");
        System.out.println("Words: " + words);

        System.out.println("\n=== Generic Methods ===");
        System.out.println(repeat("ha", 3));        // [ha, ha, ha]
        System.out.println(sum(List.of(1, 2, 3.5, 4L)));  // 10.5

        Map<String, Integer> original = Map.of("a", 1, "b", 2, "c", 3);
        System.out.println("Inverted: " + invertMap(original)); // {1=a, 2=b, 3=c}

        System.out.println("\n=== Generic Stack ===");
        GenericStack<String> stack = new GenericStack<>(10);
        stack.push("first"); stack.push("second"); stack.push("third");
        System.out.println("Peek: " + stack.peek());
        while (!stack.isEmpty()) System.out.print(stack.pop() + " ");
        System.out.println();

        System.out.println("\n=== Recursive Generic Builder ===");
        String person = new PersonBuilder().withName("Alice").withAge(30).build();
        System.out.println(person);
    }
}
