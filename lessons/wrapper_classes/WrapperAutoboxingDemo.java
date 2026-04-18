package lessons.wrapper_classes;

import java.util.ArrayList;
import java.util.List;

/**
 * LESSON 14 — Wrapper Class, Autoboxing, Unboxing (SDE2 Interview)
 */
public class WrapperAutoboxingDemo {

    public static void main(String[] args) {
        primitiveVsWrapperDemo();
        autoboxingUnboxingDemo();
        integerCacheDemo();
        nullUnboxingPitfall();
        wrapperUtilityMethods();
        performanceNoteDemo();
    }

    static void primitiveVsWrapperDemo() {
        System.out.println("=== Primitive vs Wrapper ===");

        int primitive = 10;
        Integer wrapper = Integer.valueOf(10);

        System.out.println("Primitive int: " + primitive);
        System.out.println("Wrapper Integer: " + wrapper);

        List<Integer> scores = new ArrayList<>();
        scores.add(95);
        scores.add(88);
        scores.add(76);
        System.out.println("List<Integer>: " + scores);
    }

    static void autoboxingUnboxingDemo() {
        System.out.println("\n=== Autoboxing / Unboxing ===");

        Integer boxed = 42;   // autoboxing (int -> Integer)
        int unboxed = boxed;  // unboxing (Integer -> int)

        System.out.println("Autoboxed Integer: " + boxed);
        System.out.println("Unboxed int: " + unboxed);

        Integer a = 20;
        Integer b = 22;
        int sum = a + b; // both unboxed, arithmetic on primitives
        System.out.println("Sum via unboxing: " + sum);
    }

    static void integerCacheDemo() {
        System.out.println("\n=== Integer Cache and == Pitfall ===");

        Integer x1 = 127;
        Integer x2 = 127;
        Integer y1 = 128;
        Integer y2 = 128;

        System.out.println("127 == 127: " + (x1 == x2));
        System.out.println("128 == 128: " + (y1 == y2));
        System.out.println("128 equals 128: " + y1.equals(y2));
    }

    static void nullUnboxingPitfall() {
        System.out.println("\n=== Null Unboxing Pitfall ===");

        Integer nullable = null;

        try {
            int value = nullable; // throws NPE during unboxing
            System.out.println("Value: " + value);
        } catch (NullPointerException e) {
            System.out.println("Unboxing null caused: " + e);
        }
    }

    static void wrapperUtilityMethods() {
        System.out.println("\n=== Wrapper Utility Methods ===");

        int parsed = Integer.parseInt("123");
        Integer boxed = Integer.valueOf("456");
        int compared = Integer.compare(100, 200);

        System.out.println("Integer.parseInt(\"123\"): " + parsed);
        System.out.println("Integer.valueOf(\"456\"): " + boxed);
        System.out.println("Integer.compare(100, 200): " + compared);
        System.out.println("Integer.MIN_VALUE: " + Integer.MIN_VALUE);
        System.out.println("Integer.MAX_VALUE: " + Integer.MAX_VALUE);
    }

    static void performanceNoteDemo() {
        System.out.println("\n=== Performance Note ===");

        long start = System.nanoTime();
        long primitiveSum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            primitiveSum += i;
        }
        long primitiveTime = System.nanoTime() - start;

        start = System.nanoTime();
        Long wrapperSum = 0L;
        for (long i = 0; i < 1_000_000; i++) {
            wrapperSum += i; // repeated boxing/unboxing in loop
        }
        long wrapperTime = System.nanoTime() - start;

        System.out.println("Primitive sum: " + primitiveSum + ", time: " + primitiveTime / 1_000_000 + "ms");
        System.out.println("Wrapper sum:   " + wrapperSum + ", time: " + wrapperTime / 1_000_000 + "ms");
        System.out.println("Use primitives for performance-critical numeric loops.");
    }
}