# Lesson 03 — Generics (SDE2 Interview)

## Topics Covered
1. Generic classes and methods
2. Bounded type parameters (`<T extends Comparable<T>>`)
3. Wildcards: `?`, `? extends T` (upper bounded), `? super T` (lower bounded)
4. PECS principle (Producer Extends, Consumer Super)
5. Type erasure — what it is and its implications
6. Reifiable vs non-reifiable types
7. Generic methods vs class-level generics
8. Raw types and why they're dangerous

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| What is type erasure? | JVM erases generic type info at compile time; all `List<T>` become `List` at runtime |
| `List<?>` vs `List<Object>` | `List<?>` is read-only (unknown type), `List<Object>` accepts any Object but loses type safety |
| PECS rule | Producer (you read from it) → use `extends`; Consumer (you write to it) → use `super` |
| Can you create `new T[]`? | NO — type erasure means T is unknown at runtime; use `Object[]` and cast |
| Can you catch generic exceptions? | NO — `catch(T e)` doesn't compile due to erasure |
| `List<Integer>` subtype of `List<Number>`? | NO — generics are invariant; use `List<? extends Number>` |

## Files in this Lesson
- `GenericClassDemo.java` — Generic class, methods, bounded types
- `WildcardDemo.java` — ?, extends, super wildcards + PECS
- `TypeErasureDemo.java` — Erasure, raw types, heap pollution
