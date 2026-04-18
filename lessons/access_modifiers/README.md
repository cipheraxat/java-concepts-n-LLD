# Lesson 11 — Access Modifiers (SDE2 Interview)

## Topics Covered
1. Four access levels: `private`, default (package-private), `protected`, `public`
2. Access modifier scope table
3. Private constructors (Singleton, Utility, Factory)
4. Access widening rules on method overriding
5. Access modifiers on interfaces (implicit `public`)
6. Top-level class restrictions (`public` or default only)
7. Nested/inner class access rules
8. `protected` access across packages (inheritance vs reference)

## Key Interview Points

| Concept | Quick Answer |
|---|---|
| Order (restrictive → open) | `private` → default → `protected` → `public` |
| Default access keyword? | No keyword — simply omit the modifier (package-private) |
| Top-level class modifiers? | Only `public` or default. Cannot be `private` or `protected` |
| Protected across packages? | Subclass can access via `this`/inheritance, NOT via parent reference |
| Override visibility rule? | Overriding method CANNOT be MORE restrictive than parent (Liskov) |
| Private constructor use? | Singleton, utility class, factory method, builder pattern |
| Interface method access? | Implicitly `public` (except `private` helper methods in Java 9+) |
| Interface field access? | Implicitly `public static final` |
| Inner class access? | Can access ALL outer members including `private` |
| Static nested class access? | Can access outer's `private static` members only |
| Can you override `private` methods? | No — not visible to subclass; child defines a NEW method |
| `protected` vs default? | `protected` adds subclass access in other packages; default is package-only |

## Access Scope Table

```
┌───────────┬───────┬─────────┬────────────────┬───────┐
│ Modifier  │ Class │ Package │ Subclass       │ World │
│           │       │         │ (diff package) │       │
├───────────┼───────┼─────────┼────────────────┼───────┤
│ private   │  ✅   │   ❌    │      ❌        │  ❌   │
│ default   │  ✅   │   ✅    │      ❌        │  ❌   │
│ protected │  ✅   │   ✅    │      ✅        │  ❌   │
│ public    │  ✅   │   ✅    │      ✅        │  ✅   │
└───────────┴───────┴─────────┴────────────────┴───────┘
```

## Files in this Lesson
- `AccessModifiersDemo.java` — All four modifiers, overriding rules, interfaces, nested classes, private constructors
