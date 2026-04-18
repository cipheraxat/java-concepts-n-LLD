package lessons.java8_features;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

/**
 * LESSON 06B — Stream API (Most Important Java 8 Feature for Interviews)
 *
 * Stream pipeline: Source → Intermediate ops (lazy) → Terminal op (eager, triggers execution)
 *
 * SDE2 Interview Questions:
 *  - "Can you reuse a stream?" => NO — streams are single-use. Throws IllegalStateException.
 *  - "What is the difference between map and flatMap?"
 *    => map: 1-to-1; flatMap: 1-to-0..n (flattens Stream<Stream<T>> to Stream<T>)
 *  - "When would you use reduce?" => Aggregate: sum, product, max, custom accumulation
 *  - "parallelStream() pitfalls?" => Stateful ops (sorted), shared mutable state, small datasets
 *  - "Collectors.groupingBy vs partitioningBy?"
 *    => groupingBy: group by key (Map<K, List<T>>); partitioningBy: split into true/false groups
 */
public class StreamAPIDemo {

    record Employee(String name, String dept, double salary, int age) {}

    static List<Employee> employees() {
        return List.of(
            new Employee("Alice",   "Engineering", 95000, 32),
            new Employee("Bob",     "Engineering", 87000, 28),
            new Employee("Charlie", "Marketing",   72000, 35),
            new Employee("Dave",    "Engineering",105000, 40),
            new Employee("Eve",     "HR",          65000, 27),
            new Employee("Frank",   "Marketing",   78000, 31),
            new Employee("Grace",   "HR",          70000, 29)
        );
    }

    public static void main(String[] args) {
        List<Employee> emps = employees();

        // ── Intermediate Operations ────────────────────────────────────────────
        System.out.println("=== filter + map + collect ===");
        List<String> engNames = emps.stream()
            .filter(e -> e.dept().equals("Engineering"))  // intermediate: lazy
            .map(Employee::name)                           // intermediate: lazy
            .sorted()                                      // intermediate: lazy
            .collect(Collectors.toList());                 // terminal: triggers execution
        System.out.println("Engineering: " + engNames);

        System.out.println("\n=== distinct, limit, skip ===");
        List<String> depts = emps.stream()
            .map(Employee::dept)
            .distinct()         // remove duplicates
            .sorted()
            .collect(Collectors.toList());
        System.out.println("Departments: " + depts);

        List<Employee> page = emps.stream()
            .sorted(Comparator.comparing(Employee::name))
            .skip(2)    // skip first 2
            .limit(3)   // take next 3
            .collect(Collectors.toList());
        System.out.println("Page(skip=2, limit=3): " + page.stream().map(Employee::name).toList());

        // ── flatMap ────────────────────────────────────────────────────────────
        System.out.println("\n=== flatMap ===");
        List<List<Integer>> nested = List.of(List.of(1,2,3), List.of(4,5), List.of(6,7,8,9));
        List<Integer> flat = nested.stream()
            .flatMap(Collection::stream)  // Stream<List<Integer>> → Stream<Integer>
            .collect(Collectors.toList());
        System.out.println("Flattened: " + flat);

        // Practical: Get all unique words from a list of sentences
        List<String> sentences = List.of("hello world", "java streams", "hello java");
        List<String> words = sentences.stream()
            .flatMap(s -> Arrays.stream(s.split(" ")))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        System.out.println("Unique words: " + words);

        // ── reduce ────────────────────────────────────────────────────────────
        System.out.println("\n=== reduce ===");
        double totalSalary = emps.stream()
            .mapToDouble(Employee::salary)
            .sum();       // specialized sum (avoids boxing overhead)
        System.out.println("Total salary: " + totalSalary);

        OptionalDouble avgSalary = emps.stream()
            .mapToDouble(Employee::salary)
            .average();
        System.out.println("Avg salary: " + avgSalary.orElse(0));

        // Custom reduce: product
        int factorial = IntStream.rangeClosed(1, 5)
            .reduce(1, (a, b) -> a * b);
        System.out.println("5! = " + factorial);

        // ── Collectors ────────────────────────────────────────────────────────
        System.out.println("\n=== groupingBy ===");
        Map<String, List<Employee>> byDept = emps.stream()
            .collect(Collectors.groupingBy(Employee::dept));
        byDept.forEach((dept, list) ->
            System.out.println(dept + ": " + list.stream().map(Employee::name).toList()));

        System.out.println("\n=== groupingBy with downstream collectors ===");
        // Average salary per department
        Map<String, Double> avgByDept = emps.stream()
            .collect(Collectors.groupingBy(Employee::dept,
                     Collectors.averagingDouble(Employee::salary)));
        avgByDept.forEach((d, avg) -> System.out.printf("  %s: %.0f%n", d, avg));

        // Count per department
        Map<String, Long> countByDept = emps.stream()
            .collect(Collectors.groupingBy(Employee::dept, Collectors.counting()));
        System.out.println("Count by dept: " + countByDept);

        System.out.println("\n=== partitioningBy ===");
        Map<Boolean, List<Employee>> seniorJunior = emps.stream()
            .collect(Collectors.partitioningBy(e -> e.salary() >= 80000));
        System.out.println("Senior (>=80k): " + seniorJunior.get(true).stream().map(Employee::name).toList());
        System.out.println("Junior (<80k):  " + seniorJunior.get(false).stream().map(Employee::name).toList());

        System.out.println("\n=== joining ===");
        String names = emps.stream()
            .map(Employee::name)
            .sorted()
            .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("All names: " + names);

        System.out.println("\n=== toMap ===");
        Map<String, Double> nameToSalary = emps.stream()
            .collect(Collectors.toMap(Employee::name, Employee::salary));
        System.out.println("Alice salary: " + nameToSalary.get("Alice"));

        System.out.println("\n=== min, max, count, anyMatch/allMatch/noneMatch ===");
        Employee richest = emps.stream()
            .max(Comparator.comparingDouble(Employee::salary))
            .orElseThrow();
        System.out.println("Highest earner: " + richest.name() + " $" + richest.salary());

        Employee youngest = emps.stream()
            .min(Comparator.comparingInt(Employee::age))
            .orElseThrow();
        System.out.println("Youngest: " + youngest.name() + " age " + youngest.age());

        long count = emps.stream().filter(e -> e.salary() > 80000).count();
        System.out.println("Earning >80k: " + count);

        System.out.println("Any HR? " + emps.stream().anyMatch(e -> e.dept().equals("HR")));
        System.out.println("All adults? " + emps.stream().allMatch(e -> e.age() >= 18));
        System.out.println("None in Sales? " + emps.stream().noneMatch(e -> e.dept().equals("Sales")));

        System.out.println("\n=== Primitive Streams ===");
        // IntStream, LongStream, DoubleStream avoid boxing overhead
        int[] arr = {5, 3, 1, 4, 2};
        int sum = Arrays.stream(arr).sum();
        System.out.println("Array sum: " + sum);

        IntStream.range(0, 5).forEach(i -> System.out.print(i + " "));   // 0 1 2 3 4
        System.out.println();
        IntStream.rangeClosed(1, 5).forEach(i -> System.out.print(i + " ")); // 1 2 3 4 5
        System.out.println();

        System.out.println("\n=== Parallel Stream ===");
        // Splits work across ForkJoinPool.commonPool() threads
        long parallelCount = emps.parallelStream()
            .filter(e -> e.salary() > 70000)
            .count();
        System.out.println("Parallel count (>70k): " + parallelCount);
        // WARNING: Don't use parallelStream with stateful operations or shared mutable state!

        System.out.println("\n=== Collectors.teeing (Java 12+) ===");
        // Apply two collectors and merge results
        record Stats(long count, double avg) {}
        Stats stats = emps.stream()
            .collect(Collectors.teeing(
                Collectors.counting(),
                Collectors.averagingDouble(Employee::salary),
                Stats::new
            ));
        System.out.println("Count: " + stats.count() + ", Avg salary: " + stats.avg());
    }
}
