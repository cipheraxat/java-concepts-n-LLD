# Core Java — SDE2 Interview Preparation

> Comprehensive Java lessons covering all topics tested in SDE2-level interviews.
> Each lesson contains theory, annotated code examples, and answers to common interview questions.

---

## Lesson Index

| # | Topic | Key Files | Difficulty |
|---|-------|-----------|------------|
| 01 | [OOP Concepts](01_oop_concepts/README.md) | Encapsulation, Inheritance, Polymorphism, Abstraction | ⭐⭐ |
| 02 | [Collections Framework](02_collections_framework/README.md) | List, Map, Set, Queue, Comparator | ⭐⭐⭐ |
| 03 | [Generics](03_generics/README.md) | Wildcards, PECS, Type Erasure | ⭐⭐⭐ |
| 04 | [Exception Handling](04_exception_handling/README.md) | Checked/Unchecked, try-with-resources, Custom exceptions | ⭐⭐ |
| 05 | [Multithreading & Concurrency](05_multithreading_concurrency/README.md) | Thread pools, CompletableFuture, Locks, Deadlock | ⭐⭐⭐⭐⭐ |
| 06 | [Java 8+ Features](06_java8_features/README.md) | Lambdas, Streams, Optional, Records, Switch expressions | ⭐⭐⭐⭐ |
| 07 | [Design Patterns](07_design_patterns/README.md) | Singleton, Builder, Factory, Observer, Strategy, Decorator | ⭐⭐⭐⭐ |
| 08 | [JVM Internals](08_jvm_internals/README.md) | Memory areas, GC, ClassLoader, JIT, JMM | ⭐⭐⭐⭐ |
| 09 | [String Handling](09_string_handling/README.md) | Immutability, StringBuilder, String pool, Regex | ⭐⭐ |
| 10 | [SOLID Principles](10_solid_principles/README.md) | SRP, OCP, LSP, ISP, DIP with code | ⭐⭐⭐ |
| 11 | [Access Modifiers](11_access_modifiers/README.md) | private, default, protected, public, overriding rules | ⭐⭐ |
| 12 | [static & final Keywords](12_static_and_final/README.md) | static fields/methods/blocks, final variables/methods/classes, effectively final | ⭐⭐⭐ |
| 13 | [Inner Classes](inner_classes/README.md) | Member inner, static nested, local inner, anonymous inner class | ⭐⭐⭐ |
| 14 | [Wrapper Class, Autoboxing, Unboxing](wrapper_classes/README.md) | Primitive vs wrapper, Integer cache, boxing/unboxing pitfalls | ⭐⭐ |

---

## SDE2 Interview Focus Areas

### Must-Know (High Frequency)

```
✅ HashMap internals (hashing, collision, Java 8 treeification, load factor)
✅ Java Memory Model (volatile, happens-before, synchronized)
✅ Thread pool configuration (core/max/queue/rejection policy)
✅ CompletableFuture chaining (thenApply/thenCompose/thenCombine/exceptionally)
✅ Stream API (groupingBy, flatMap, collect, reduce)
✅ equals + hashCode contract
✅ Generics wildcards (PECS rule)
✅ Singleton patterns (enum, DCL, holder)
✅ SOLID + Design Patterns (explain + code)
✅ Checked vs Unchecked exceptions, try-with-resources
```

### Common Interview Questions by Category

#### OOP
- What is the difference between abstract class and interface?
- Can we override static/private/final methods?
- What is dynamic method dispatch?
- Why override `hashCode` when you override `equals`?

#### Collections
- How does `HashMap` work internally?
- `HashMap` vs `ConcurrentHashMap` vs `Hashtable`?
- When to use `TreeMap` vs `HashMap`?
- What is fail-fast vs fail-safe iterator?

#### Concurrency
- What is a race condition? How to fix it?
- `synchronized` vs `ReentrantLock`?
- `volatile` vs `AtomicInteger`?
- How to implement a thread-safe singleton?
- Producer-consumer using `BlockingQueue`?
- What causes a deadlock? How to prevent it?

#### Java 8+
- Explain Java 8 Stream pipeline (lazy evaluation)
- `map` vs `flatMap`?
- `Optional.orElse` vs `orElseGet`?
- What are functional interfaces?
- What are Java Records? When to use them?

#### JVM
- What is the difference between Heap and Stack?
- What is GC pausing (stop-the-world)?
- Why use G1GC over Parallel GC?
- What is type erasure?

#### Design Patterns
- How to prevent Singleton from being broken by reflection?
- Builder vs Constructor?
- When to use Strategy vs if-else?
- What is the difference between Proxy and Decorator?

---

## Suggested Study Order

**Week 1:** OOP (01) → Collections (02) → Strings (09) → Exceptions (04)
**Week 2:** Java 8+ (06) → Generics (03) → SOLID (10)
**Week 3:** Concurrency (05) → Design Patterns (07) → JVM (08)

---

## How to Run Examples

```bash
# Compile all lessons (from project root)
javac --enable-preview --release 21 -d out \
  lessons/**/*.java

# Run a specific demo
java --enable-preview -cp out lessons.oop_concepts.PolymorphismDemo
java --enable-preview -cp out lessons.collections_framework.MapDemo
java --enable-preview -cp out lessons.java8_features.StreamAPIDemo
java --enable-preview -cp out lessons.multithreading_concurrency.ConcurrentUtilsDemo
java --enable-preview -cp out lessons.inner_classes.InnerClassesDemo
```

> **Note:** `--enable-preview` is needed for sealed classes and some modern Java features.  
> Tested with Java 17 and Java 21.
