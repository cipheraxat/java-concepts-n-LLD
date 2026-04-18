# Lesson 06 — Java 8+ Features (SDE2 Interview)

## Topics Covered
1. Lambda expressions — syntax, effectively final, capturing variables
2. Functional interfaces — Function, Predicate, Consumer, Supplier, BiFunction
3. Method references — static, instance, constructor
4. Stream API — intermediate vs terminal, lazy evaluation, parallel streams
5. Optional — avoiding NPE, map/filter/orElse/orElseThrow
6. Default and static interface methods
7. Date/Time API (java.time) — LocalDate, LocalDateTime, ZonedDateTime, Duration
8. Java 9-17 additions: `var`, records, sealed classes, text blocks, switch expressions

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| Lambda vs anonymous class | Lambda: no `this` capture, no shadow variables, more concise. Anonymous class has own `this`. |
| What is a functional interface? | Interface with exactly one abstract method. `@FunctionalInterface` enforces this. |
| Lazy evaluation in Streams | Intermediate ops (filter/map) only execute when terminal op (collect/count) is called |
| `Stream.of()` vs `Arrays.stream()` | Arrays.stream supports primitives (IntStream); Stream.of boxes them |
| `flatMap` vs `map` | map: 1-to-1 transform; flatMap: 1-to-many (flattens nested streams) |
| `Optional.get()` pitfall | Throws NoSuchElementException if empty. Use `orElse`/`orElseThrow`/`ifPresent` instead |
| `parallelStream()` when? | CPU-bound tasks on large collections. NOT IO-bound or stateful ops. Overhead for small collections |

## Files in this Lesson
- `LambdaFunctionalDemo.java` — Lambdas, functional interfaces, method references
- `StreamAPIDemo.java` — Full Stream API operations
- `OptionalDemo.java` — Optional patterns and pitfalls
- `DateTimeDemo.java` — java.time API
- `ModernJavaDemo.java` — Records, sealed classes, var, switch expressions, text blocks
