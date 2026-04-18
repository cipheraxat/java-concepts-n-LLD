# Lesson 01 — OOP Concepts (SDE2 Interview)

## Topics Covered
1. Encapsulation
2. Inheritance (single, multi-level, hierarchical)
3. Polymorphism (compile-time & runtime)
4. Abstraction (abstract class vs interface)
5. Association, Aggregation, Composition
6. `final`, `static`, `this`, `super` keywords
7. Method Overloading vs Overriding rules
8. Covariant return types
9. Constructor chaining
10. Object class methods (`equals`, `hashCode`, `toString`, `clone`)

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| Abstraction vs Encapsulation | Abstraction hides *what* (design); Encapsulation hides *how* (implementation/data) |
| Abstract class vs Interface | Abstract class has state + partial impl; Interface is a pure contract (Java 8+ allows default methods) |
| Overloading vs Overriding | Overloading = same name, diff params (compile-time); Overriding = same sig in subclass (runtime) |
| Why override `hashCode` with `equals`? | Contract: equal objects must have same hash. Breaks HashMap/Set otherwise |
| `IS-A` vs `HAS-A` | IS-A = Inheritance; HAS-A = Composition/Aggregation |
| Can constructors be inherited? | No. But `super()` calls parent constructor |
| Can we override `static` methods? | No — static methods are hidden, not overridden (no runtime polymorphism) |
| Can we override `private` methods? | No — private methods are not visible to subclass |

## Files in this Lesson
- `EncapsulationDemo.java` — Getter/setter, data hiding
- `InheritanceDemo.java` — Single, multi-level, method hiding
- `PolymorphismDemo.java` — Overloading, overriding, dynamic dispatch
- `AbstractionDemo.java` — Abstract class vs Interface
- `InterfaceDemo.java` — Interface contract, default methods, multiple implementation, marker interface
- `CompositionDemo.java` — Composition vs Aggregation vs Association
- `ObjectMethodsDemo.java` — equals, hashCode, toString, clone
