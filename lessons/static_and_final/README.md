# Lesson 12 — Mastering static & final Keywords (SDE2 Interview)

## Topics Covered

### static
1. Static fields (class variables) — shared state, memory location
2. Static methods — no `this`, utility methods, `main()`
3. Static blocks (class initializers) — execution order, use cases
4. Static nested class vs inner class — memory, access rules
5. Static method hiding (NOT overriding) — compile-time resolution
6. Static methods in interfaces (Java 8+)
7. Static and serialization (static fields are NOT serialized)

### final
1. Final variables — primitives vs references (immutability misconception)
2. Blank final variables — assigned once in constructor
3. Final methods — cannot be overridden, template method pattern
4. Final classes — cannot be subclassed (`String`, `Integer`)
5. Final parameters — prevents reassignment in method body
6. Effectively final (Java 8+) — lambda capture rules

### static + final Combined
1. Constants (`static final`) — naming convention, constant pool, inlining
2. Immutable vs unmodifiable collections
3. Static factory with cached final instances
4. Static initializer ordering gotchas

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| `static` field shared? | YES — one copy per class, shared across all instances |
| Can `static` method access instance vars? | NO — no `this` reference in static context |
| Can we override `static` methods? | NO — they are hidden, not overridden (compile-time dispatch) |
| Why is `main()` static? | JVM calls it without creating an instance |
| `static` fields serialized? | NO — they belong to the class, not the instance |
| Multiple static blocks? | YES — executed in declaration order when class is loaded |
| Static nested vs inner class? | Static nested has no outer `this`; inner holds implicit ref to outer |
| `final` variable = immutable? | NO — reference can't be reassigned, but object CAN be mutated |
| Blank final? | Final field not initialized at declaration; must be assigned in every constructor |
| Can we override `final` method? | NO — compile error |
| Why is `String` final? | Immutability + security (String pool, class loading) + thread safety |
| `static final` primitive inlined? | YES — compiler replaces references with the literal value |
| Effectively final? | A variable never reassigned after init; can be captured by lambdas |
| Final class benefits? | Immutability guarantee, security, design clarity |
| Immutable vs unmodifiable? | Unmodifiable is a read-only wrapper; original can still be mutated |

## Cheat Sheet

```
┌─────────────────────────────────────────────────────────────────┐
│                     static                                      │
├─────────────────────────────────────────────────────────────────┤
│ static field     → shared class-level variable                  │
│ static method    → no this, cannot access instance members      │
│ static block     → runs once at class loading                   │
│ static nested    → no outer.this reference, lower memory        │
│ static import    → import static java.lang.Math.PI;             │
├─────────────────────────────────────────────────────────────────┤
│                      final                                      │
├─────────────────────────────────────────────────────────────────┤
│ final variable   → cannot reassign (object still mutable!)      │
│ final method     → cannot be overridden                         │
│ final class      → cannot be subclassed                         │
│ final parameter  → cannot reassign inside method                │
│ blank final      → must initialize in every constructor         │
│ effectively final→ never reassigned → usable in lambdas         │
├─────────────────────────────────────────────────────────────────┤
│                  static + final                                 │
├─────────────────────────────────────────────────────────────────┤
│ static final     → compile-time constant, UPPER_SNAKE_CASE      │
│ static final ref → reference fixed, object may be mutable       │
│ Use List.of()    → truly immutable static final collection      │
└─────────────────────────────────────────────────────────────────┘
```

## Files in this Lesson
- `StaticFinalDemo.java` — Static fields/methods/blocks, final variables/methods/classes, combined patterns, interview traps
