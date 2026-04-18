# Lesson 13 — Inner Classes in Java (SDE2 Interview)

## Topics Covered
1. Member inner class (non-static)
2. Static nested class
3. Local inner class (inside method)
4. Anonymous inner class
5. Access rules between outer and inner classes
6. Compilation output (`Outer$Inner.class`)
7. `this` and `OuterClass.this` usage
8. Memory implications and best practices

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| Inner class vs static nested class | Inner class has implicit reference to outer instance; static nested class does not |
| Can inner class access private members of outer class? | YES |
| Can static nested class access outer instance fields directly? | NO |
| How to create member inner class object? | `Outer.Inner in = outer.new Inner();` |
| Can local variables be used in local/anonymous classes? | YES, but they must be final or effectively final |
| Why are anonymous classes still relevant with lambdas? | Needed when implementing abstract classes or multiple methods/state |
| Memory concern with inner classes | Inner class retains outer reference, which can lead to leaks if misused |
| Bytecode naming | Compiler generates `Outer$Inner.class`, `Outer$1.class`, etc. |

## Quick Comparison

```
┌────────────────────────┬──────────────────────────────┬──────────────────────────────┐
│ Type                   │ Outer Instance Reference     │ Typical Use                  │
├────────────────────────┼──────────────────────────────┼──────────────────────────────┤
│ Member Inner           │ YES                          │ Tight coupling with outer    │
│ Static Nested          │ NO                           │ Utility/helper grouping      │
│ Local Inner            │ YES (within method scope)    │ Short-lived method logic     │
│ Anonymous Inner        │ YES (if non-static context)  │ One-off implementation       │
└────────────────────────┴──────────────────────────────┴──────────────────────────────┘
```

## Files in this Lesson
- `InnerClassesDemo.java` — End-to-end runnable examples of all inner class types with interview-focused notes
