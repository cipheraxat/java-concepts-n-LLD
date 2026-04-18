# Lesson 14 — Wrapper Class, Autoboxing, Unboxing (SDE2 Interview)

## Topics Covered
1. Primitive types vs Wrapper classes
2. Why wrapper classes exist in Java
3. Autoboxing and unboxing (compiler-generated conversions)
4. Integer cache (`-128` to `127`) and `==` pitfalls
5. Null unboxing and `NullPointerException`
6. Wrapper utilities (`parseXxx`, `valueOf`, `compare`, `MIN_VALUE`, `MAX_VALUE`)
7. Performance considerations in loops and collections

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| Why wrappers? | Collections and generics only work with objects, not primitives. |
| Autoboxing | Automatic conversion from primitive to wrapper, e.g. `int` -> `Integer`. |
| Unboxing | Automatic conversion from wrapper to primitive, e.g. `Integer` -> `int`. |
| `Integer a = 127; Integer b = 127; a == b` | `true` due to Integer cache (`-128` to `127`). |
| `Integer c = 128; Integer d = 128; c == d` | Usually `false` (different objects). |
| Wrapper comparison best practice | Use `equals()` for value comparison, not `==`. |
| Risk with unboxing | `Integer x = null; int y = x;` throws `NullPointerException`. |
| `parseInt` vs `valueOf` | `parseInt` returns primitive `int`; `valueOf` returns `Integer`. |

## Files in this Lesson
- `WrapperAutoboxingDemo.java` — End-to-end examples of wrappers, autoboxing, unboxing, common pitfalls, and interview patterns.

## Quick Notes
- Prefer primitives for heavy numeric computation when null is not needed.
- Use wrappers when APIs require objects (`List<Integer>`, `Map<Long, String>`, etc.).
- Be careful with autoboxing in tight loops due to object creation overhead.