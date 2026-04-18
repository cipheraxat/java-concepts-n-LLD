package lessons.java8_features;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * LESSON 06A — Lambda Expressions + Functional Interfaces + Method References
 *
 * SDE2 Interview Questions:
 *  - "What can a lambda capture from enclosing scope?"
 *    => Local variables that are effectively final (not modified after assignment)
 *  - "Difference between lambda and anonymous class?"
 *    => Lambda: no 'this', no shadowing. Anonymous class: has own 'this', can shadow outer variables
 *  - "What are the core functional interfaces?"
 *    => Function<T,R>, Predicate<T>, Consumer<T>, Supplier<T>, BiFunction<T,U,R>, UnaryOperator<T>
 */
public class LambdaFunctionalDemo {

    // ─── 1. Lambda syntax evolution ──────────────────────────────────────────────
    @FunctionalInterface
    interface MathOperation {
        int operate(int a, int b);
    }

    // ─── 2. Core Functional Interfaces ───────────────────────────────────────────

    static void coreFunctionalInterfaces() {
        // Function<T, R>: takes T, returns R
        Function<String, Integer> strLen = String::length;
        Function<Integer, String> intToStr = i -> "Number: " + i;
        // Compose: apply strLen first, then multiply
        Function<String, Integer> lenTimes2 = strLen.andThen(n -> n * 2);
        System.out.println("andThen: " + lenTimes2.apply("Hello"));    // 10
        Function<String, Integer> composed = strLen.compose(String::trim); // trim first, then strLen
        System.out.println("compose: " + composed.apply("  Hello  ")); // 5

        // Predicate<T>: takes T, returns boolean
        Predicate<String> notEmpty   = s -> !s.isEmpty();
        Predicate<String> longerThan5 = s -> s.length() > 5;
        Predicate<String> combined   = notEmpty.and(longerThan5);
        Predicate<String> either     = notEmpty.or(s -> s.equals("!"));
        Predicate<String> negated    = notEmpty.negate();
        System.out.println("'Hello World' combined: " + combined.test("Hello World")); // true
        System.out.println("'' negated: " + negated.test(""));   // false

        // Consumer<T>: takes T, returns void
        Consumer<String> printer    = System.out::println;
        Consumer<String> uppPrinter = s -> System.out.println(s.toUpperCase());
        Consumer<String> printBoth  = printer.andThen(uppPrinter);
        printBoth.accept("hello");  // prints "hello" then "HELLO"

        // Supplier<T>: takes nothing, returns T
        Supplier<List<String>> listFactory = ArrayList::new;
        List<String> newList = listFactory.get();
        newList.add("test");
        System.out.println("Supplier: " + newList);

        // BiFunction<T, U, R>
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println("BiFunction: " + repeat.apply("ab", 3)); // ababab

        // UnaryOperator<T> (extends Function<T,T>): same input/output type
        UnaryOperator<String> toUpper = String::toUpperCase;
        System.out.println("UnaryOperator: " + toUpper.apply("hello")); // HELLO

        // BinaryOperator<T> (BiFunction<T,T,T>)
        BinaryOperator<Integer> max = (a, b) -> a > b ? a : b;
        System.out.println("BinaryOperator: " + max.apply(5, 3)); // 5
    }

    // ─── 3. Method References ─────────────────────────────────────────────────────
    static void methodReferences() {
        System.out.println("\n=== Method References ===");

        // Static method reference: ClassName::staticMethod
        Function<String, Integer> parseInt = Integer::parseInt;
        System.out.println("Static: " + parseInt.apply("42")); // 42

        // Instance method reference on arbitrary instance: ClassName::instanceMethod
        Function<String, String> toUpper = String::toUpperCase;
        List.of("a", "b", "c").stream().map(toUpper).forEach(System.out::println);

        // Instance method reference on specific instance: instance::instanceMethod
        String prefix = "Hello, ";
        Function<String, String> greet = prefix::concat;  // effectively final
        System.out.println("Instance: " + greet.apply("Alice")); // Hello, Alice

        // Constructor reference: ClassName::new
        Function<String, StringBuilder> sbFactory = StringBuilder::new;
        StringBuilder sb = sbFactory.apply("initial");
        System.out.println("Constructor ref: " + sb);

        // Array constructor reference (IntFunction is correct for one-dimensional arrays)
        java.util.function.IntFunction<String[]> arrFactory = String[]::new;
        String[] arr = arrFactory.apply(5); // new String[5]
        System.out.println("Array factory length: " + arr.length);
    }

    // ─── 4. Lambda capturing variables ────────────────────────────────────────────
    static void capturingDemo() {
        System.out.println("\n=== Lambda Variable Capture ===");
        int multiplier = 5;  // effectively final — cannot reassign after lambda uses it
        // multiplier = 10; // would cause compile error if uncommented
        Function<Integer, Integer> multiply = n -> n * multiplier;  // captures multiplier
        System.out.println("Captured: " + multiply.apply(6)); // 30

        // Instance fields: accessible (captured via this reference)
        // Static fields: accessible (no capture needed)

        // Lambda does NOT capture 'this' in the same way as anonymous classes:
        // in static context, use class literal instead.
        Runnable lambda = () -> System.out.println("Lambda 'this': " + LambdaFunctionalDemo.class.getName());
        // Runnable anon = new Runnable() {
        //     public void run() {
        //         System.out.println(this.getClass().getName()); // Runnable$$Anonymous, not LambdaFunctionalDemo
        //     }
        // };
        lambda.run();
    }

    // ─── 5. Lambda as MathOperation ──────────────────────────────────────────────
    static int apply(int a, int b, MathOperation op) {
        return op.operate(a, b);
    }

    public static void main(String[] args) {
        System.out.println("=== Lambda Syntax ===");
        // No params
        Runnable r = () -> System.out.println("No params");
        r.run();

        // One param (parens optional for single param)
        Function<String, String> upper = s -> s.toUpperCase();
        System.out.println(upper.apply("hello"));

        // Multiple params, block body
        MathOperation add = (a, b) -> a + b;
        MathOperation pow = (a, b) -> {
            int result = 1;
            for (int i = 0; i < b; i++) result *= a;
            return result;
        };
        System.out.println("add(3,4): " + apply(3, 4, add));  // 7
        System.out.println("pow(2,8): " + apply(2, 8, pow));  // 256

        System.out.println("\n=== Core Functional Interfaces ===");
        coreFunctionalInterfaces();

        methodReferences();
        capturingDemo();
    }
}
